package com.llmplatform.plugin.rag.ingest;

import java.util.ArrayList;
import java.util.List;

/**
 * 按空行切段；超长段落再滑窗。第 1 周只保证“能入库、能按段召回”。
 */
public final class TextChunker {

    static final int MAX_CHARS = 400;
    static final int OVERLAP = 80;

    public List<String> split(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        List<String> chunks = new ArrayList<>();
        for (String paragraph : text.split("\\R\\s*\\R")) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() <= MAX_CHARS) {
                chunks.add(trimmed);
            } else {
                chunks.addAll(windows(trimmed));
            }
        }
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        return List.copyOf(chunks);
    }

    private static List<String> windows(String text) {
        List<String> pieces = new ArrayList<>();
        int step = MAX_CHARS - OVERLAP;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(text.length(), start + MAX_CHARS);
            String piece = text.substring(start, end).trim();
            if (!piece.isEmpty()) {
                pieces.add(piece);
            }
            if (end == text.length()) {
                break;
            }
        }
        return pieces;
    }
}
