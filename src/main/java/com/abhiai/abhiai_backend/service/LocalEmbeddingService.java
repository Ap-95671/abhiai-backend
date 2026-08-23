package com.abhiai.abhiai_backend.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class LocalEmbeddingService {

    private static final int DIMENSIONS = 256;

    public String embed(String text) {
        double[] vector = vector(text);
        StringBuilder serialized = new StringBuilder();
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) serialized.append(',');
            serialized.append(vector[index]);
        }
        return serialized.toString();
    }

    public double similarity(String text, String serializedEmbedding) {
        if (serializedEmbedding == null || serializedEmbedding.isBlank()) return 0;
        String[] values = serializedEmbedding.split(",");
        if (values.length != DIMENSIONS) return 0;
        double[] right = new double[DIMENSIONS];
        try {
            for (int index = 0; index < values.length; index++) {
                right[index] = Double.parseDouble(values[index]);
            }
        } catch (NumberFormatException exception) {
            return 0;
        }
        double[] left = vector(text);
        double dot = 0;
        for (int index = 0; index < DIMENSIONS; index++) dot += left[index] * right[index];
        return dot;
    }

    private double[] vector(String text) {
        double[] result = new double[DIMENSIONS];
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.length() < 2) continue;
            int hash = token.hashCode();
            int index = Math.floorMod(hash, DIMENSIONS);
            result[index] += (hash & 1) == 0 ? 1 : -1;
        }
        double magnitude = 0;
        for (double value : result) magnitude += value * value;
        magnitude = Math.sqrt(magnitude);
        if (magnitude > 0) {
            for (int index = 0; index < result.length; index++) result[index] /= magnitude;
        }
        return result;
    }
}
