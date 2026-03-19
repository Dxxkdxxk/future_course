package com.lzlz.springboot.security.service;

public interface StudentCourseAccessService {

    void checkCourseAccess(Integer studentId, Long courseId);

    Long checkTaskAccess(Integer studentId, Long taskId);

    Long checkHomeworkAccess(Integer studentId, Long courseId, Long homeworkId);

    Long checkGraphAccess(Integer studentId, Long courseId, Long graphId);
}
