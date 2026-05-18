package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.GraphBuildResponse;
import com.lzlz.springboot.security.dto.NodeBindingDto;
import com.lzlz.springboot.security.dto.VideoProgressDto;

public interface GraphLearningProgressService {
    GraphBuildResponse fillStudentProgress(Long courseId, Long graphId, Integer studentId, GraphBuildResponse response);

    GraphBuildResponse fillClassAverageProgress(Long courseId, Long graphId, GraphBuildResponse response);

    VideoProgressDto.ReportResponse reportVideoProgress(Long courseId, Long graphId, String nodeId, Integer studentId, VideoProgressDto.ReportRequest request);

    void bindNodeTask(Long courseId, Long graphId, String nodeId, NodeBindingDto.UpsertRequest request);

    NodeBindingDto.BindingListResponse listNodeBindings(Long courseId, Long graphId, String nodeId);

    void removeNodeBinding(Long courseId, Long graphId, String nodeId, NodeBindingDto.RemoveRequest request);

    void recalculateStudentByHomework(Long courseId, Long homeworkId, Integer studentId);

    void recalculateStudentByExamTask(Long courseId, Long taskId, Integer studentId);

    void recalculateAllStudentsForNode(Long courseId, Long graphId, String nodeId);
}
