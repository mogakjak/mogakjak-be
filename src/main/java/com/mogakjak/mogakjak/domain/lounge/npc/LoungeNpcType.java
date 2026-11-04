package com.mogakjak.mogakjak.domain.lounge.npc;

import java.util.List;

public enum LoungeNpcType {
    A("모각이", 50, 90, 15,
            List.of("코딩테스트 준비", "자격증 공부하기", "사이드 프로젝트 작업", "보고서 작성")),
    B("집중이", 25, 50, 30,
            List.of("토익 공부하기", "포트폴리오 수정하기", "자기소개서 작성하기", "전공 공부")),
    C("동동이", 15, 35, 60,
            List.of("독서하기", "업무 계획 세우기", "아이디어 정리", "다이어리 쓰기"));

    private final String nickname;
    private final int minActivitySeconds;
    private final int maxActivitySeconds;
    private final int taskChangePercent;
    private final List<String> tasks;

    LoungeNpcType(String nickname, int minMinutes, int maxMinutes, int taskChangePercent, List<String> tasks) {
        this.nickname = nickname;
        this.minActivitySeconds = minMinutes * 60;
        this.maxActivitySeconds = maxMinutes * 60;
        this.taskChangePercent = taskChangePercent;
        this.tasks = tasks;
    }

    public String nickname() { return nickname; }
    public int minActivitySeconds() { return minActivitySeconds; }
    public int maxActivitySeconds() { return maxActivitySeconds; }
    public int taskChangePercent() { return taskChangePercent; }
    public List<String> tasks() { return tasks; }
}
