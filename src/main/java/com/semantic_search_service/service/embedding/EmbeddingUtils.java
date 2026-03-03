package com.semantic_search_service.service.embedding;

public class EmbeddingUtils {
    private EmbeddingUtils() {
    }

    public static float[] l2Normalized(float[] v) {
        double sumOfSquares = 0.0;
        for (float f : v) sumOfSquares += (double) f * f;

        double norm = Math.sqrt(sumOfSquares);
        if (norm == 0.0) return v.clone();

        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / norm);
        }
        return out;
    }
}
