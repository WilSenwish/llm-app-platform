package com.llmplatform.plugin.rag.embed;

import java.util.Locale;

/**
 * 确定性哈希向量，用作第 1 周最小检索闭环的嵌入模型。
 * <p>把字符二元组做特征哈希，映射到固定维度。相同文本得到相同向量，重叠字面越多余弦越高。
 * 它不是语义模型：换一种说法可能召不回。第 2 周模型网关接入后再替换本实现。
 */
public final class HashingEmbeddingModel {

    /** 向量维度。固定下来，避免和已入库向量长度不一致。 */
    public static final int DIMENSION = 1024;

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        float[] vector = new float[DIMENSION];
        int[] points = text.toLowerCase(Locale.ROOT).codePoints().toArray();
        for (int i = 0; i < points.length; i++) {
            accumulate(vector, points[i]);
            if (i + 1 < points.length) {
                accumulate(vector, points[i] * 31 + points[i + 1]);
            }
        }
        normalize(vector);
        return vector;
    }

    /** 余弦相似度，范围约 [-1, 1]。零向量返回 0。 */
    public static double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            dot += left[i] * (double) right[i];
            leftNorm += left[i] * (double) left[i];
            rightNorm += right[i] * (double) right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static void accumulate(float[] vector, int feature) {
        int bucket = Math.floorMod(feature, vector.length);
        int sign = Math.floorMod(feature * 0x9E3779B9, 2) == 0 ? 1 : -1;
        vector[bucket] += sign;
    }

    private static void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= (float) norm;
        }
    }
}
