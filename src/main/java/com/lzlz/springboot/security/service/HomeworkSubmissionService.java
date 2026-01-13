package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.dto.StudentSubmissionDto;
import com.lzlz.springboot.security.entity.HomeworkSubmission;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface HomeworkSubmissionService {

    /**
     * 学生提交作业
     */
    void submitHomework(Long courseId, Long homeworkId, Long studentId, MultipartFile[] files, String content);
    /**
     * 获取我的提交记录 (学生查详情)
     */
    HomeworkSubmission getMySubmission(Long homeworkId, Long studentId);

    /**
     * 获取某作业的提交名单 (教师端 - 聚合了文件链接和学生信息)
     */
    List<StudentSubmissionDto> getHomeworkSubmissionList(Long courseId, Long homeworkId);

    // 建议加上 courseId，以利用智能校验

    // 建议加上 courseId
    void gradeSubmission(Long courseId, Long submissionId, Integer finalScore, String teacherComment);


    /**
     * [新增] 获取单个提交记录的详情 (用于阅卷)
     *@param submissionId 提交记录ID
     *@return 包含文件链接和学生信息的DTO
     */
    StudentSubmissionDto getSubmissionDetail(Long courseId, Long submissionId);
}