package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.GraphBuildResponse;
import com.lzlz.springboot.security.dto.GraphNode;
import com.lzlz.springboot.security.dto.GraphNodeBindingSummary;
import com.lzlz.springboot.security.dto.GraphNodeProgress;
import com.lzlz.springboot.security.dto.NodeBindingDto;
import com.lzlz.springboot.security.dto.VideoProgressDto;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.entity.HomeworkSubmission;
import com.lzlz.springboot.security.entity.StudentPaperRecord;
import com.lzlz.springboot.security.entity.TestTask;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.entity.ClassStudent;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.mapper.ClassStudentMapper;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.mapper.HomeworkSubmissionMapper;
import com.lzlz.springboot.security.mapper.StudentPaperRecordMapper;
import com.lzlz.springboot.security.mapper.TestTaskMapper;
import com.lzlz.springboot.security.repository.GraphRepository;
import com.lzlz.springboot.security.service.GraphLearningProgressService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class GraphLearningProgressServiceImpl implements GraphLearningProgressService {

    private final GraphRepository graphRepository;
    private final TestTaskMapper testTaskMapper;
    private final StudentPaperRecordMapper studentPaperRecordMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final CourseMapper courseMapper;
    private final ClassStudentMapper classStudentMapper;

    public GraphLearningProgressServiceImpl(GraphRepository graphRepository,
                                            TestTaskMapper testTaskMapper,
                                            StudentPaperRecordMapper studentPaperRecordMapper,
                                            HomeworkMapper homeworkMapper,
                                            HomeworkSubmissionMapper homeworkSubmissionMapper,
                                            CourseMapper courseMapper,
                                            ClassStudentMapper classStudentMapper) {
        this.graphRepository = graphRepository;
        this.testTaskMapper = testTaskMapper;
        this.studentPaperRecordMapper = studentPaperRecordMapper;
        this.homeworkMapper = homeworkMapper;
        this.homeworkSubmissionMapper = homeworkSubmissionMapper;
        this.courseMapper = courseMapper;
        this.classStudentMapper = classStudentMapper;
    }

    @Override
    public GraphBuildResponse fillStudentProgress(Long courseId, Long graphId, Integer studentId, GraphBuildResponse response) {
        if (response == null || response.getNodes() == null || response.getNodes().isEmpty()) {
            return response;
        }
        for (GraphNode node : response.getNodes()) {
            GraphNodeProgress progress = buildNodeProgress(courseId, graphId, node.getNodeId(), studentId);
            node.setProgress(progress);
            node.setBindingSummary(buildBindingSummary(courseId, graphId, node.getNodeId()));
        }
        return response;
    }

    @Override
    public GraphBuildResponse fillClassAverageProgress(Long courseId, Long graphId, GraphBuildResponse response) {
        if (response == null || response.getNodes() == null || response.getNodes().isEmpty()) {
            return response;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getClassId() == null) {
            return response;
        }
        QueryWrapper<ClassStudent> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", course.getClassId());
        List<ClassStudent> students = classStudentMapper.selectList(wrapper);
        if (students == null || students.isEmpty()) {
            return response;
        }

        for (GraphNode node : response.getNodes()) {
            double videoSum = 0.0d;
            double examSum = 0.0d;
            double homeworkSum = 0.0d;
            double overallSum = 0.0d;
            int count = 0;
            for (ClassStudent student : students) {
                if (student.getUserId() == null) {
                    continue;
                }
                GraphNodeProgress p = buildNodeProgress(courseId, graphId, node.getNodeId(), student.getUserId());
                videoSum += p.getVideoProgress() == null ? 0.0d : p.getVideoProgress();
                examSum += p.getExamProgress() == null ? 0.0d : p.getExamProgress();
                homeworkSum += p.getHomeworkProgress() == null ? 0.0d : p.getHomeworkProgress();
                overallSum += p.getOverallProgress() == null ? 0.0d : p.getOverallProgress();
                count++;
            }
            double divisor = count <= 0 ? 1.0d : count;
            node.setProgress(GraphNodeProgress.builder()
                    .videoProgress(round2(videoSum / divisor))
                    .examProgress(round2(examSum / divisor))
                    .homeworkProgress(round2(homeworkSum / divisor))
                    .overallProgress(round2(overallSum / divisor))
                    .updatedAt(LocalDateTime.now())
                    .build());
            node.setBindingSummary(buildBindingSummary(courseId, graphId, node.getNodeId()));
        }
        return response;
    }

    @Override
    public VideoProgressDto.ReportResponse reportVideoProgress(Long courseId, Long graphId, String nodeId, Integer studentId, VideoProgressDto.ReportRequest request) {
        if (request.getResourceId() == null || request.getResourceId().isBlank()) {
            throw new CustomGraphException(400, "resourceId is required");
        }
        int watched = request.getWatchedSeconds() == null ? 0 : request.getWatchedSeconds();
        int duration = request.getDurationSeconds() == null ? 0 : request.getDurationSeconds();
        graphRepository.upsertVideoProgress(graphId, nodeId, courseId, studentId, request.getResourceId(), watched, duration);

        GraphNodeProgress progress = recalculateNodeProgress(courseId, graphId, nodeId, studentId);
        VideoProgressDto.ReportResponse response = new VideoProgressDto.ReportResponse();
        response.setNodeId(nodeId);
        response.setProgress(progress);
        return response;
    }

    @Override
    public void bindNodeTask(Long courseId, Long graphId, String nodeId, NodeBindingDto.UpsertRequest request) {
        String taskType = normalizeTaskType(request.getTaskType());
        if (request.getTaskId() == null) {
            throw new CustomGraphException(400, "taskId is required");
        }
        graphRepository.bindTaskToNode(graphId, nodeId, courseId, taskType, request.getTaskId(), request.getWeight());
        recalculateAllStudentsForNode(courseId, graphId, nodeId);
    }

    @Override
    public NodeBindingDto.BindingListResponse listNodeBindings(Long courseId, Long graphId, String nodeId) {
        List<Map<String, Object>> rows = graphRepository.listNodeBindings(graphId, nodeId, courseId);
        List<NodeBindingDto.BindingItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            NodeBindingDto.BindingItem item = new NodeBindingDto.BindingItem();
            String relType = Objects.toString(row.get("relType"), "");
            item.setTaskType("BINDS_EXAM".equals(relType) ? "EXAM" : "HOMEWORK");
            item.setTaskId(toLong(row.get("taskId")));
            item.setWeight(toDouble(row.get("weight"), 1.0d));
            items.add(item);
        }
        NodeBindingDto.BindingListResponse response = new NodeBindingDto.BindingListResponse();
        response.setNodeId(nodeId);
        response.setBindings(items);
        return response;
    }

    @Override
    public void removeNodeBinding(Long courseId, Long graphId, String nodeId, NodeBindingDto.RemoveRequest request) {
        if (request.getTaskId() == null) {
            throw new CustomGraphException(400, "taskId is required");
        }
        String taskType = normalizeTaskType(request.getTaskType());
        graphRepository.removeNodeBinding(graphId, nodeId, courseId, taskType, request.getTaskId());
        recalculateAllStudentsForNode(courseId, graphId, nodeId);
    }

    @Override
    public void recalculateStudentByHomework(Long courseId, Long homeworkId, Integer studentId) {
        List<Map<String, Object>> nodes = graphRepository.listBoundNodesForTask(courseId, "HOMEWORK", homeworkId);
        for (Map<String, Object> row : nodes) {
            Long graphId = toLong(row.get("graphId"));
            String nodeId = Objects.toString(row.get("nodeId"), null);
            if (graphId != null && nodeId != null) {
                recalculateNodeProgress(courseId, graphId, nodeId, studentId);
            }
        }
    }

    @Override
    public void recalculateStudentByExamTask(Long courseId, Long taskId, Integer studentId) {
        List<Map<String, Object>> nodes = graphRepository.listBoundNodesForTask(courseId, "EXAM", taskId);
        for (Map<String, Object> row : nodes) {
            Long graphId = toLong(row.get("graphId"));
            String nodeId = Objects.toString(row.get("nodeId"), null);
            if (graphId != null && nodeId != null) {
                recalculateNodeProgress(courseId, graphId, nodeId, studentId);
            }
        }
    }

    @Override
    public void recalculateAllStudentsForNode(Long courseId, Long graphId, String nodeId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getClassId() == null) {
            return;
        }
        QueryWrapper<ClassStudent> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", course.getClassId());
        List<ClassStudent> students = classStudentMapper.selectList(wrapper);
        for (ClassStudent student : students) {
            if (student.getUserId() != null) {
                recalculateNodeProgress(courseId, graphId, nodeId, student.getUserId());
            }
        }
    }

    public GraphNodeProgress recalculateNodeProgress(Long courseId, Long graphId, String nodeId, Integer studentId) {
        double video = graphRepository.calculateNodeVideoProgress(graphId, nodeId, courseId, studentId);
        double exam = calculateExamProgress(courseId, graphId, nodeId, studentId);
        double homework = calculateHomeworkProgress(courseId, graphId, nodeId, studentId);
        double overall = round2(video * 0.4d + exam * 0.3d + homework * 0.3d);
        GraphNodeProgress progress = GraphNodeProgress.builder()
                .videoProgress(round2(video))
                .examProgress(round2(exam))
                .homeworkProgress(round2(homework))
                .overallProgress(overall)
                .updatedAt(LocalDateTime.now())
                .build();
        graphRepository.upsertNodeProgress(graphId, nodeId, courseId, studentId,
                progress.getVideoProgress(), progress.getExamProgress(), progress.getHomeworkProgress(), progress.getOverallProgress());
        propagateProgressToAncestors(courseId, graphId, nodeId, studentId);
        return progress;
    }

    private void propagateProgressToAncestors(Long courseId, Long graphId, String nodeId, Integer studentId) {
        List<String> ancestors = graphRepository.listAncestorNodeIds(graphId, nodeId);
        for (String ancestorNodeId : ancestors) {
            Map<String, Double> rollup = graphRepository.calculateParentProgressFromChildren(graphId, ancestorNodeId, courseId, studentId);
            double video = round2(rollup.getOrDefault("videoProgress", 0.0d));
            double exam = round2(rollup.getOrDefault("examProgress", 0.0d));
            double homework = round2(rollup.getOrDefault("homeworkProgress", 0.0d));
            double overall = round2(rollup.getOrDefault("overallProgress", 0.0d));
            graphRepository.upsertNodeProgress(graphId, ancestorNodeId, courseId, studentId, video, exam, homework, overall);
        }
    }

    private GraphNodeProgress buildNodeProgress(Long courseId, Long graphId, String nodeId, Integer studentId) {
        Map<String, Object> row = graphRepository.getNodeProgress(graphId, nodeId, courseId, studentId);
        if (row == null) {
            return GraphNodeProgress.builder()
                    .videoProgress(0.0d)
                    .examProgress(0.0d)
                    .homeworkProgress(0.0d)
                    .overallProgress(0.0d)
                    .build();
        }
        return GraphNodeProgress.builder()
                .videoProgress(toDouble(row.get("videoProgress"), 0.0d))
                .examProgress(toDouble(row.get("examProgress"), 0.0d))
                .homeworkProgress(toDouble(row.get("homeworkProgress"), 0.0d))
                .overallProgress(toDouble(row.get("overallProgress"), 0.0d))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private GraphNodeBindingSummary buildBindingSummary(Long courseId, Long graphId, String nodeId) {
        int videoCount = graphRepository.countNodeVideos(graphId, nodeId);
        int examCount = graphRepository.listNodeBindingsByType(graphId, nodeId, courseId, "EXAM").size();
        int homeworkCount = graphRepository.listNodeBindingsByType(graphId, nodeId, courseId, "HOMEWORK").size();
        return GraphNodeBindingSummary.builder()
                .videoCount(videoCount)
                .examCount(examCount)
                .homeworkCount(homeworkCount)
                .build();
    }

    private double calculateExamProgress(Long courseId, Long graphId, String nodeId, Integer studentId) {
        List<Map<String, Object>> bindings = graphRepository.listNodeBindingsByType(graphId, nodeId, courseId, "EXAM");
        if (bindings.isEmpty()) {
            return 0.0d;
        }
        double weighted = 0.0d;
        double weightSum = 0.0d;
        for (Map<String, Object> binding : bindings) {
            Long taskId = toLong(binding.get("taskId"));
            if (taskId == null) {
                continue;
            }
            TestTask task = testTaskMapper.selectById(taskId);
            if (task == null || task.getPaperId() == null) {
                continue;
            }
            QueryWrapper<StudentPaperRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("paper_id", task.getPaperId()).eq("student_id", studentId).orderByDesc("created_at").last("LIMIT 1");
            StudentPaperRecord record = studentPaperRecordMapper.selectOne(wrapper);
            if (record == null || record.getFullScore() == null || record.getFullScore() <= 0 || record.getTotalScore() == null) {
                continue;
            }
            double scorePct = (record.getTotalScore() * 100.0d) / record.getFullScore();
            double w = toDouble(binding.get("weight"), 1.0d);
            weighted += scorePct * w;
            weightSum += w;
        }
        if (weightSum <= 0) {
            return 0.0d;
        }
        return weighted / weightSum;
    }

    private double calculateHomeworkProgress(Long courseId, Long graphId, String nodeId, Integer studentId) {
        List<Map<String, Object>> bindings = graphRepository.listNodeBindingsByType(graphId, nodeId, courseId, "HOMEWORK");
        if (bindings.isEmpty()) {
            return 0.0d;
        }
        double weighted = 0.0d;
        double weightSum = 0.0d;
        for (Map<String, Object> binding : bindings) {
            Long homeworkId = toLong(binding.get("taskId"));
            if (homeworkId == null) {
                continue;
            }
            Homework homework = homeworkMapper.selectById(homeworkId);
            if (homework == null || homework.getTotalScore() == null || homework.getTotalScore() <= 0) {
                continue;
            }
            QueryWrapper<HomeworkSubmission> wrapper = new QueryWrapper<>();
            wrapper.eq("homework_id", homeworkId).eq("student_id", studentId).last("LIMIT 1");
            HomeworkSubmission submission = homeworkSubmissionMapper.selectOne(wrapper);
            if (submission == null || submission.getFinalScore() == null) {
                continue;
            }
            double scorePct = (submission.getFinalScore() * 100.0d) / homework.getTotalScore();
            double w = toDouble(binding.get("weight"), 1.0d);
            weighted += scorePct * w;
            weightSum += w;
        }
        if (weightSum <= 0) {
            return 0.0d;
        }
        return weighted / weightSum;
    }

    private String normalizeTaskType(String taskType) {
        if (taskType == null) {
            throw new CustomGraphException(400, "taskType is required");
        }
        String normalized = taskType.trim().toUpperCase(Locale.ROOT);
        if (!"EXAM".equals(normalized) && !"HOMEWORK".equals(normalized)) {
            throw new CustomGraphException(400, "taskType must be EXAM or HOMEWORK");
        }
        return normalized;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
