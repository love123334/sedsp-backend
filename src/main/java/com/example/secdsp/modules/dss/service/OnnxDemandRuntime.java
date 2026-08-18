package com.example.secdsp.modules.dss.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.Map;

/**
 * Isolates ONNX native classloading. {@link LightGbmOnnxDemandPredictor} must not
 * import these types — OrtEnvironment static-init can crash Spring Boot on Railway
 * even when the model is only used later.
 */
@Slf4j
final class OnnxDemandRuntime {

    private static volatile OrtEnvironment environment;
    private static volatile OrtSession session;

    private OnnxDemandRuntime() {}

    static double predict(Path modelPath, float[] features) throws Exception {
        OrtSession activeSession = sessionFor(modelPath);
        String inputName = activeSession.getInputNames().iterator().next();
        try (
            OnnxTensor input = OnnxTensor.createTensor(
                environment(),
                new float[][] { features }
            );
            OrtSession.Result result = activeSession.run(Map.of(inputName, input))
        ) {
            return LightGbmOnnxDemandPredictor.extractPrediction(result.get(0).getValue());
        }
    }

    static void closeQuietly() {
        OrtSession existing = session;
        session = null;
        if (existing == null) {
            return;
        }
        try {
            existing.close();
        } catch (OrtException exception) {
            log.debug("Cannot close ONNX session cleanly", exception);
        }
    }

    private static OrtSession sessionFor(Path modelPath) throws OrtException {
        OrtSession existing = session;
        if (existing != null) {
            return existing;
        }
        synchronized (OnnxDemandRuntime.class) {
            if (session == null) {
                session = environment().createSession(modelPath.toString());
                log.info("Loaded global LightGBM ONNX demand model");
            }
            return session;
        }
    }

    private static OrtEnvironment environment() {
        OrtEnvironment existing = environment;
        if (existing != null) {
            return existing;
        }
        synchronized (OnnxDemandRuntime.class) {
            if (environment == null) {
                environment = OrtEnvironment.getEnvironment();
            }
            return environment;
        }
    }
}
