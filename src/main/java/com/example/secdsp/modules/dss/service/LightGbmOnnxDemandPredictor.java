package com.example.secdsp.modules.dss.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Component
@Slf4j
public class LightGbmOnnxDemandPredictor {

    private static final String GLOBAL_MODEL_FILE = "global-demand.onnx";

    static final List<String> FEATURE_NAMES = List.of(
        "history_days",
        "day_of_week",
        "day_of_month",
        "month",
        "is_weekend",
        "lag_1",
        "lag_7",
        "rolling_mean_7",
        "rolling_mean_14",
        "rolling_mean_30",
        "rolling_std_7",
        "momentum_7",
        "trend_slope_14"
    );

    private final Path modelDirectory;
    private volatile OrtEnvironment environment;
    private volatile OrtSession session;
    private volatile boolean modelFailed;

    public LightGbmOnnxDemandPredictor(
        @Value("${app.dss.model-dir:models/demand}") String modelDirectory
    ) {
        this.modelDirectory = Path.of(modelDirectory).toAbsolutePath().normalize();
    }

    public boolean isModelAvailable(Long productId) {
        return productId != null && Files.isRegularFile(modelPath());
    }

    public OptionalDouble predict(
        Long productId,
        LocalDate targetDate,
        List<Long> history,
        int historyDays
    ) {
        if (!isModelAvailable(productId) || modelFailed) {
            return OptionalDouble.empty();
        }

        try {
            OrtSession session = sessionFor();
            String inputName = session.getInputNames().iterator().next();
            float[] features = buildFeatures(targetDate, history, historyDays);
            OrtEnvironment runtimeEnvironment = environment();

            try (
                OnnxTensor input = OnnxTensor.createTensor(
                    runtimeEnvironment,
                    new float[][] { features }
                );
                OrtSession.Result result = session.run(Map.of(inputName, input))
            ) {
                double prediction = extractPrediction(result.get(0).getValue());
                if (!Double.isFinite(prediction)) {
                    return OptionalDouble.empty();
                }
                return OptionalDouble.of(Math.max(0.0, prediction));
            }
        } catch (Exception | LinkageError exception) {
            modelFailed = true;
            log.warn(
                "Cannot run global LightGBM ONNX model for product {}. Falling back to baseline.",
                productId,
                exception
            );
            return OptionalDouble.empty();
        }
    }

    float[] buildFeatures(
        LocalDate targetDate,
        List<Long> completeHistory,
        int requestedHistoryDays
    ) {
        int historyDays = Math.max(1, requestedHistoryDays);
        int start = Math.max(0, completeHistory.size() - historyDays);
        List<Long> history = new ArrayList<>(
            completeHistory.subList(start, completeHistory.size())
        );

        double recentAverage = averageOfTail(history, 7);
        double previousAverage = averageOfPreviousTail(history, 7);

        return new float[] {
            historyDays,
            targetDate.getDayOfWeek().getValue(),
            targetDate.getDayOfMonth(),
            targetDate.getMonthValue(),
            targetDate.getDayOfWeek().getValue() >= 6 ? 1.0f : 0.0f,
            (float) lag(history, 1),
            (float) lag(history, 7),
            (float) recentAverage,
            (float) averageOfTail(history, 14),
            (float) averageOfTail(history, 30),
            (float) standardDeviationOfTail(history, 7),
            (float) (recentAverage - previousAverage),
            (float) linearRegressionSlopeOfTail(history, 14)
        };
    }

    private OrtSession sessionFor() throws OrtException {
        OrtSession existing = session;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (session == null) {
                session = environment().createSession(modelPath().toString());
                log.info("Loaded global LightGBM ONNX demand model");
            }
            return session;
        }
    }

    private OrtEnvironment environment() {
        OrtEnvironment existing = environment;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (environment == null) {
                environment = OrtEnvironment.getEnvironment();
            }
            return environment;
        }
    }

    private Path modelPath() {
        return modelDirectory.resolve(GLOBAL_MODEL_FILE);
    }

    static double extractPrediction(Object output) {
        if (output instanceof Number value) {
            return value.doubleValue();
        }
        if (output != null && output.getClass().isArray()) {
            int length = Array.getLength(output);
            if (length > 0) {
                return extractPrediction(Array.get(output, 0));
            }
        }
        throw new IllegalStateException(
            "Unsupported ONNX output type: "
                + (output == null ? "null" : output.getClass().getName())
        );
    }

    private static double lag(List<Long> history, int days) {
        int index = history.size() - days;
        return index >= 0 ? history.get(index) : 0.0;
    }

    private static double averageOfTail(List<Long> history, int window) {
        if (history.isEmpty()) {
            return 0.0;
        }
        int start = Math.max(0, history.size() - window);
        return history.subList(start, history.size())
            .stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    private static double averageOfPreviousTail(List<Long> history, int window) {
        int end = Math.max(0, history.size() - window);
        if (end == 0) {
            return averageOfTail(history, window);
        }
        int start = Math.max(0, end - window);
        return history.subList(start, end)
            .stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    private static double standardDeviationOfTail(List<Long> history, int window) {
        if (history.isEmpty()) {
            return 0.0;
        }
        int start = Math.max(0, history.size() - window);
        List<Long> tail = history.subList(start, history.size());
        double average = tail.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        double variance = tail.stream()
            .mapToDouble(value -> Math.pow(value - average, 2))
            .average()
            .orElse(0.0);
        return Math.sqrt(variance);
    }

    private static double linearRegressionSlopeOfTail(
        List<Long> history,
        int window
    ) {
        int start = Math.max(0, history.size() - window);
        List<Long> tail = history.subList(start, history.size());
        int size = tail.size();
        if (size < 2) {
            return 0.0;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;
        for (int index = 0; index < size; index++) {
            double x = index + 1.0;
            double y = tail.get(index);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denominator = (size * sumX2) - (sumX * sumX);
        return denominator == 0.0
            ? 0.0
            : ((size * sumXY) - (sumX * sumY)) / denominator;
    }

    @PreDestroy
    public void close() {
        OrtSession existing = session;
        if (existing != null) {
            try {
                existing.close();
            } catch (OrtException exception) {
                log.debug("Cannot close ONNX session cleanly", exception);
            }
            session = null;
        }
    }
}
