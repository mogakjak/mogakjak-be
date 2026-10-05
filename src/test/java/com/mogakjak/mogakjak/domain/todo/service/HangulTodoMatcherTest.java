package com.mogakjak.mogakjak.domain.todo.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class HangulTodoMatcherTest {
    static Stream<Arguments> matchingRules() {
        return Stream.of(
                Arguments.of("ㄱ", "가방 정리", true),
                Arguments.of("ㄱ", "각오", true),
                Arguments.of("ㄱ", "까치", false),
                Arguments.of("ㅎ", "힣", true),
                Arguments.of("가", "가방", true),
                Arguments.of("가", "각방", false),
                Arguments.of("ㄱㅂ", "가방 정리", true),
                Arguments.of("ㄱㅂ", "가나다", false),
                Arguments.of("ㄱㅂ", "가 방", false),
                Arguments.of("ㄱ ㅂ", "가 방", true),
                Arguments.of("가ㅂ", "가방", true),
                Arguments.of("가ㅂ", "각방", false),
                Arguments.of("ㄱ방", "공방", true),
                Arguments.of(" ㄱㅂ ", "가방", true),
                Arguments.of("", "완료한 일", true),
                Arguments.of("　", "어떤 일", true),
                Arguments.of("ᄀ", "공부", true),
                Arguments.of("가", "가방", true),
                Arguments.of("ㄱㅏ", "가방", true),
                Arguments.of("가", "가방", true),
                Arguments.of("가ᄇ", "가방", true),
                Arguments.of("간", "간식", true),
                Arguments.of("java", "JAVA 공부", true),
                Arguments.of("Ａ", "abc", true),
                Arguments.of("%", "100% 목표", true),
                Arguments.of("%", "abc", false),
                Arguments.of("_", "x_y", true),
                Arguments.of("_", "xyz", false),
                Arguments.of(".*", "a.*b", true),
                Arguments.of(".*", "abc", false),
                Arguments.of("[", "[독서]", true),
                Arguments.of("'", "작가's 책", true),
                Arguments.of("📚", "책 📚 읽기", true),
                Arguments.of("ㄱ📚", "가📚", true),
                Arguments.of("ㅏ", "가방", false),
                Arguments.of("ㅏ", "모음 ㅏ", true),
                Arguments.of("없는검색어", "가방", false)
        );
    }

    @ParameterizedTest
    @MethodSource("matchingRules")
    void matchesContiguousLiteralAndInitialTokens(String query, String title, boolean expected) {
        assertEquals(expected, new HangulTodoMatcher(query).matches(title));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"})
    void allNineteenInitialsMatchSyllablesWithVowelsAndFinalConsonants(String initial) {
        int index = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".indexOf(initial);
        String syllable = new String(Character.toChars(0xAC00 + index * 588 + 10 * 28 + 7));
        assertTrue(new HangulTodoMatcher(initial).matches(syllable));
    }
}
