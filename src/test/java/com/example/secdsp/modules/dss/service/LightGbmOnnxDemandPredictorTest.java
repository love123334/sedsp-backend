package com.example.secdsp.modules.dss.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightGbmOnnxDemandPredictorTest {

    @Test
    void buildsFeaturesInTheSameOrderAsTrainingPipeline() {
        LightGbmOnnxDemandPredictor predictor =
            new LightGbmOnnxDemandPredictor("models/does-not-exist");

        float[] features = predictor.buildFeatures(
            LocalDate.of(2026, 8, 17),
            List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L),
            14
        );

        assertEquals(LightGbmOnnxDemandPredictor.FEATURE_NAMES.size(), features.length);
        assertArrayEquals(
            new float[] {
                14.0f,
                1.0f,
                17.0f,
                8.0f,
                0.0f,
                14.0f,
                8.0f,
                11.0f,
                7.5f,
                7.5f,
                2.0f,
                7.0f,
                1.0f
            },
            features,
            0.0001f
        );
    }

    @Test
    void failsStartupWhenRequiredModelIsMissing() {
        LightGbmOnnxDemandPredictor predictor =
            new LightGbmOnnxDemandPredictor("models/does-not-exist", true);

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            predictor::validateModelOnStartup
        );
        assertTrue(
            exception.getMessage().contains("Demand model file not found")
                || exception.getMessage().contains("Demand ONNX runtime is required")
        );
    }

    @Test
    void extractsPredictionFromNestedObjectAndPrimitiveArrays() {
        assertEquals(
            12.75,
            LightGbmOnnxDemandPredictor.extractPrediction(
                new Object[] { new float[] { 12.75f } }
            ),
            0.0001
        );
        assertEquals(
            8.5,
            LightGbmOnnxDemandPredictor.extractPrediction(
                new Object[] { new Object[] { new double[] { 8.5 } } }
            ),
            0.0001
        );
    }

    @Test
    void runsGeneratedGlobalLightGbmOnnxModel() {
        LightGbmOnnxDemandPredictor predictor =
            new LightGbmOnnxDemandPredictor("models/demand");

        var prediction = predictor.predict(
            65L,
            LocalDate.of(2026, 8, 18),
            List.of(
                4L, 5L, 0L, 4L, 5L, 6L, 0L,
                5L, 6L, 5L, 0L, 7L, 5L, 6L
            ),
            14
        );

        Assumptions.assumeTrue(
            prediction.isPresent(),
            "ONNX native runtime is unavailable on this machine"
        );
        assertTrue(prediction.getAsDouble() >= 0.0);
        predictor.close();
    }
}
