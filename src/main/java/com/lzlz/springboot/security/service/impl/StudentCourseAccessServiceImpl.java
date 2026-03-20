package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.entity.ClassStudent;
import com.lzlz.springboot.security.entity.Course;
import com.lzlz.springboot.security.entity.GraphMetadata;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.entity.TestTask;
import com.lzlz.springboot.security.exception.CustomGraphException;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.ClassStudentMapper;
import com.lzlz.springboot.security.mapper.CourseMapper;
import com.lzlz.springboot.security.mapper.GraphMetadataMapper;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.mapper.TestTaskMapper;
import com.lzlz.springboot.security.service.StudentCourseAccessService;
import org.springframework.stereotype.Service;

@Service
public class StudentCourseAccessServiceImpl implements StudentCourseAccessService {

    private final CourseMapper courseMapper;
    private final ClassStudentMapper classStudentMapper;
    private final TestTaskMapper testTaskMapper;
    private final HomeworkMapper homeworkMapper;
    private final GraphMetadataMapper graphMetadataMapper;

    public StudentCourseAccessServiceImpl(CourseMapper courseMapper,
                                          ClassStudentMapper classStudentMapper,
                                          TestTaskMapper testTaskMapper,
                                          HomeworkMapper homeworkMapper,
                                          GraphMetadataMapper graphMetadataMapper) {
        this.courseMapper = courseMapper;
        this.classStudentMapper = classStudentMapper;
        this.testTaskMapper = testTaskMapper;
        this.homeworkMapper = homeworkMapper;
        this.graphMetadataMapper = graphMetadataMapper;
    }

    @Override
    public void checkCourseAccess(Integer studentId, Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        if (course.getClassId() == null) {
            throw new CustomGraphException(403, "无权访问该课程");
        }

        QueryWrapper<ClassStudent> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", course.getClassId()).eq("user_id", studentId);
        if (classStudentMapper.selectCount(wrapper) == 0) {
            throw new CustomGraphException(403, "无权访问该课程");
        }
    }

    @Override
    public Long checkTaskAccess(Integer studentId, Long taskId) {
        TestTask task = testTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId);
        }
        checkCourseAccess(studentId, task.getCourseId());
        return task.getCourseId();
    }

    @Override
    public Long checkHomeworkAccess(Integer studentId, Long courseId, Long homeworkId) {
        Homework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new ResourceNotFoundException("Homework not found with id: " + homeworkId);
        }
        if (courseId != null && !homework.getCourseId().equals(courseId)) {
            throw new CustomGraphException(400, "作业不属于当前课程");
        }
        checkCourseAccess(studentId, homework.getCourseId());
        return homework.getCourseId();
    }

    @Override
    public Long checkGraphAccess(Integer studentId, Long courseId, Long graphId) {
        GraphMetadata graphMetadata = graphMetadataMapper.selectById(graphId);
        if (graphMetadata == null) {
            throw new ResourceNotFoundException("Graph not found with id: " + graphId);
        }
        if (courseId != null && graphMetadata.getCourseId() != courseId) {
            throw new CustomGraphException(400, "图谱不属于当前课程");
        }
        checkCourseAccess(studentId, graphMetadata.getCourseId());
        return graphMetadata.getCourseId();
    }
}
