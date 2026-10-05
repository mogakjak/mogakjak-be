package com.mogakjak.mogakjak.domain.todo.service;

import java.text.Normalizer;
import java.util.Locale;

public final class HangulTodoMatcher {
    private final int[] query;

    public HangulTodoMatcher(String keyword) {
        query = normalize(keyword).strip().codePoints().toArray();
    }

    public boolean matches(String title) {
        if (query.length == 0) return true;
        int[] text = normalize(title).codePoints().toArray();
        for (int start = 0; start <= text.length - query.length; start++) {
            boolean matched = true;
            for (int i = 0; i < query.length; i++) {
                int token = query[i];
                if (token >= 0x1100 && token <= 0x1112) {
                    if (initialIndex(text[start + i]) != token - 0x1100) {
                        matched = false;
                        break;
                    }
                } else if (token != text[start + i]) {
                    matched = false;
                    break;
                }
            }
            if (matched) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    private static int initialIndex(int codePoint) {
        if (codePoint >= 0xAC00 && codePoint <= 0xD7A3) return (codePoint - 0xAC00) / 588;
        if (codePoint >= 0x1100 && codePoint <= 0x1112) return codePoint - 0x1100;
        return -1;
    }
}
