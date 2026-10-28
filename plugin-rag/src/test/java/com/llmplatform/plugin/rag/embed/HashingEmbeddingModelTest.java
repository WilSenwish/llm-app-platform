package com.llmplatform.plugin.rag.embed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashingEmbeddingModelTest {

    private final HashingEmbeddingModel model = new HashingEmbeddingModel();

    @Test
    void sameTextProducesSameUnitVector() {
        float[] first = model.embed("插件注册中心加载 RAG");
        float[] second = model.embed("插件注册中心加载 RAG");

        assertEquals(HashingEmbeddingModel.DIMENSION, first.length);
        assertEquals(1.0, norm(first), 1e-5);
        for (int i = 0; i < first.length; i++) {
            assertEquals(first[i], second[i]);
        }
    }

    @Test
    void relatedTextIsCloserThanUnrelatedText() {
        float[] document = model.embed("平台内核通过插件注册中心加载 RAG 插件。");
        float[] related = model.embed("RAG 插件如何被加载");
        float[] unrelated = model.embed("quantum chromodynamics lattice");

        assertTrue(HashingEmbeddingModel.cosine(document, related)
                > HashingEmbeddingModel.cosine(document, unrelated));
    }

    @Test
    void blankTextIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> model.embed("  "));
    }

    private static double norm(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
}
