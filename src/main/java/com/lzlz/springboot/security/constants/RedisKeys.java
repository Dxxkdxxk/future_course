package com.lzlz.springboot.security.constants;

public final class RedisKeys {
    private RedisKeys() {
    }

    public static String courseList() {
        return "course:list";
    }

    public static String courseDetail(Long courseId) {
        return "course:detail:" + courseId;
    }

    public static String homeworkList(Long courseId) {
        return "homework:list:course:" + courseId;
    }

    public static String homeworkTeacherDetail(Long courseId, Long homeworkId) {
        return "homework:detail:teacher:" + courseId + ":" + homeworkId;
    }

    public static String classList() {
        return "class:list";
    }

    public static String questionList(Long courseId, String querySignature) {
        return "question:list:course:" + courseId + ":q:" + querySignature;
    }
}
