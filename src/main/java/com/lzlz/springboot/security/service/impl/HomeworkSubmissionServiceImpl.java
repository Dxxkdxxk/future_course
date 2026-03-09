package com.lzlz.springboot.security.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.StudentSubmissionDto;
import com.lzlz.springboot.security.entity.HomeworkSubmission;
import com.lzlz.springboot.security.mapper.HomeworkSubmissionMapper;
import com.lzlz.springboot.security.mapper.UserMapper;
import com.lzlz.springboot.security.security.User;
import com.lzlz.springboot.security.service.HomeworkSubmissionService;
import com.lzlz.springboot.security.service.MinIOService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.entity.Homework;
import com.lzlz.springboot.security.mapper.HomeworkMapper;
import com.lzlz.springboot.security.exception.CustomGraphException; // 使用自定义异常

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HomeworkSubmissionServiceImpl implements HomeworkSubmissionService {

    @Autowired
    private HomeworkSubmissionMapper submissionMapper;

    @Autowired
    private HomeworkMapper homeworkMapper; // [新增] 注入它来查作业归属

    @Autowired
    private MinIOService minIOService;

    @Autowired
    private UserMapper userMapper;


    /**
     * [核心智能校验方法]
     * 根据传入的 ID 进行层级匹配校验。参数如果为 null 则跳过对应层级的校验。
     *
     *
     @param
     courseId     课程ID (可选)
     *
     @param
     homeworkId   作业ID (可选)
     *
     @param
     submissionId 提交ID (可选)
     */
    private void validateRelation(Long courseId, Long homeworkId, Long submissionId)
    {
        // 1. 检查提交记录 (如果提供了 submissionId)
        if (submissionId != null) {
            HomeworkSubmission sub = submissionMapper.selectById(submissionId);
            if (sub == null) {
                throw new ResourceNotFoundException("找不到提交记录: " + submissionId);
            }
            // 校验: 提交是否属于该作业
            if (homeworkId != null && !sub.getHomeworkId().equals(homeworkId)) {
                throw new CustomGraphException(400, "参数冲突: 提交记录(" + submissionId + ") 不属于作业(" + homeworkId + ")");
            }
            // 关键点: 即使入参 homeworkId 是 null，我们现在也知道了真正的 homeworkId，可以用于后续查 course
            homeworkId = sub.getHomeworkId();
        }

        // 2. 检查作业记录 (如果提供了 homeworkId，或者从上面步骤获取到了)
        if (homeworkId != null) {
            Homework hw = homeworkMapper.selectById(homeworkId);
            if (hw == null) {
                throw new ResourceNotFoundException("找不到作业: " + homeworkId);
            }
            // 校验: 作业是否属于该课程
            if (courseId != null && !hw.getCourseId().equals(courseId)) {
                throw new CustomGraphException(400, "参数冲突: 作业(" + homeworkId + ") 不属于课程(" + courseId + ")");
            }
        }
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitHomework(Long courseId,Long homeworkId, int studentId, MultipartFile[] files, String content) {
        validateRelation(courseId, homeworkId, null);
        StringBuilder objectNames = new StringBuilder();
        System.out.println(files.length);
        // 1. 循环上传文件到 MinIO
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    try {
                        // 上传单个文件
                        String name = minIOService.uploadFile(file);
                        // 拼接文件名 (用逗号分隔)
                        if (objectNames.length() > 0) {
                            objectNames.append(",");
                        }
                        objectNames.append(name);
                    } catch (Exception e) {
                        log.error("MinIO upload failed", e);
                        throw new RuntimeException("文件上传失败: " + file.getOriginalFilename());
                    }
                }
            }
        }

        String finalAttachmentUrls = objectNames.toString(); // 结果形如: "uuid1.jpg,uuid2.pdf"

        // 2. 存入数据库
        HomeworkSubmission existing = getMySubmission(homeworkId, studentId);
        if (existing != null) {
            // 覆盖更新
            // 如果这次没传文件(files为空)，是否要清空之前的？通常是：传了新文件就覆盖，没传就保留或清空，看业务。
            // 这里假设：只要调用了接口，就以当前传的为准
            if (!finalAttachmentUrls.isEmpty()) {
                existing.setAttachmentUrls(finalAttachmentUrls);
            }
            // existing.setContent(content);
            existing.setSubmittedAt(LocalDateTime.now());
            existing.setStatus(1);
            submissionMapper.updateById(existing);
        } else {
            // 新增
            HomeworkSubmission submission = new HomeworkSubmission();
            submission.setHomeworkId(homeworkId);
            submission.setStudentId(studentId);
            submission.setAttachmentUrls(finalAttachmentUrls);
            // submission.setContent(content);
            submission.setSubmittedAt(LocalDateTime.now());
            submission.setStatus(1);
            submissionMapper.insert(submission);
        }
    }

    @Override
    public HomeworkSubmission getMySubmission(Long homeworkId, int studentId) {
        QueryWrapper<HomeworkSubmission> wrapper = new QueryWrapper<>();
        wrapper.eq("homework_id", homeworkId);
        wrapper.eq("student_id", studentId);
        return submissionMapper.selectOne(wrapper);
    }

    @Override
    public List<StudentSubmissionDto> getHomeworkSubmissionList(Long courseId, Long homeworkId) {
        validateRelation(courseId, homeworkId, null);
        // 1. 查提交记录
        QueryWrapper<HomeworkSubmission> wrapper = new QueryWrapper<>();
        wrapper.eq("homework_id", homeworkId);
        wrapper.orderByDesc("submitted_at");
        List<HomeworkSubmission> list = submissionMapper.selectList(wrapper);

        if (list.isEmpty()) return Collections.emptyList();

        // 2. 查学生信息
// 2. 查学生信息
// (前提：实体类 s.getStudentId() 和 User.getId() 都已修改为 Integer 类型)
        List<Integer> studentIds = list.stream()
                .map(s -> s.getStudentId()) // 直接获取 Integer，不需要 .intValue() 或强转
                .distinct()
                .collect(Collectors.toList());

// 批量查询 (MyBatis-Plus 会自动处理 List<Integer>)
        List<User> students = userMapper.selectBatchIds(studentIds);

// 转为 Map
// (!!!) 修改点：Key 的类型统一为 Integer，不再强转为 Long
        Map<Integer, User> studentMap = students.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 3. 组装数据 & 生成链接
        List<StudentSubmissionDto> dtos = new ArrayList<>();
        for (HomeworkSubmission sub : list) {
            StudentSubmissionDto dto = new StudentSubmissionDto();
            dto.setSubmissionId(sub.getId());
            dto.setStudentId(sub.getStudentId());
            dto.setStatus(sub.getStatus());
            dto.setFinalScore(sub.getFinalScore());
            dto.setTeacherComment(sub.getTeacherComment());
            dto.setSubmittedAt(sub.getSubmittedAt());
            dto.setGradedAt(sub.getGradedAt());

            User u = studentMap.get(sub.getStudentId());
            if (u != null) {
                dto.setStudentName(u.getUsername());
                dto.setStudentEmail(u.getEmail());
            }

            // ... 在 for 循环内部 ...

            // [修改点] 处理多文件链接
            String objectNameStr = sub.getAttachmentUrls(); // 数据库里是 "a.jpg,b.png"
            List<String> urls = new ArrayList<>();

            if (objectNameStr != null && !objectNameStr.isEmpty()) {
                String[] names = objectNameStr.split(","); // 按逗号拆分
                for (String name : names) {
                    try {
                        if (!name.trim().isEmpty()) {
                            // 为每个文件生成预览链接
                            String url = minIOService.getPresignedUrl(name.trim());
                            urls.add(url);
                        }
                    } catch (Exception e) {
                        urls.add("Error: " + name);
                    }
                }
            }
            // 设置到 DTO (DTO里已经是 List<String> 了)
            dto.setAttachmentUrls(urls);

            // ...
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gradeSubmission(Long courseId, Long submissionId, Integer finalScore, String teacherComment) {
        // 1. 安全校验
        // 传入 courseId 和 submissionId，validateRelation 会自动查找 homeworkId 并校验三者层级关系
        // 如果 submissionId 不属于 courseId 下的作业，这里会直接抛出异常
        validateRelation(courseId, null, submissionId);

        // 2. 查询记录
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            // 理论上 validateRelation 已经查过了，这里是为了双重保险
            throw new ResourceNotFoundException("找不到提交记录: " + submissionId);
        }

        // 3. 更新字段
        submission.setFinalScore(finalScore);
        submission.setTeacherComment(teacherComment);

        // 更新状态为 "2: 已批改"
        submission.setStatus(2);

        // 更新批改时间
        submission.setGradedAt(LocalDateTime.now());

        // 4. 保存
        submissionMapper.updateById(submission);
    }


    @Override
    public StudentSubmissionDto getSubmissionDetail(Long courseId,Long submissionId) {
        validateRelation(courseId, null, submissionId);
        // 1. 查询提交记录
        HomeworkSubmission sub = submissionMapper.selectById(submissionId);
        if (sub == null) {
            throw new ResourceNotFoundException("Submission not found with id: " + submissionId);
        }

        validateRelation(courseId, null, submissionId);
        StudentSubmissionDto dto = new StudentSubmissionDto();

        // 2. 填充基础信息
        dto.setSubmissionId(sub.getId());
        dto.setStudentId(sub.getStudentId());
        dto.setStatus(sub.getStatus());
        dto.setFinalScore(sub.getFinalScore());
        dto.setTeacherComment(sub.getTeacherComment());
        dto.setSubmittedAt(sub.getSubmittedAt());
        dto.setGradedAt(sub.getGradedAt());
        // 如果您的实体类里有 content 字段 (学生备注)，请取消注释下一行
        // dto.setContent(sub.getContent());

        // 3. 填充学生信息 (查询 User 表)
        // 注意：如果您的 User ID 还是 Integer，请使用 Long.valueOf() 转换或对应修改
        User student = userMapper.selectById(sub.getStudentId());
        if (student != null) {
            dto.setStudentName(student.getUsername());
            dto.setStudentEmail(student.getEmail());
        } else {
            dto.setStudentName("未知学生");
        }

        // 4. [核心] 处理多文件链接 (MinIO)
        String objectNameStr = sub.getAttachmentUrls(); // 数据库存的是 "uuid1.jpg,uuid2.png"
        List<String> urls = new ArrayList<>();

        if (objectNameStr != null && !objectNameStr.isEmpty()) {
            String[] names = objectNameStr.split(",");
            for (String name : names) {
                if (!name.trim().isEmpty()) {
                    try {
                        // 生成临时可访问的 HTTP 链接
                        String url = minIOService.getPresignedUrl(name.trim());
                        urls.add(url);
                    } catch (Exception e) {
                        log.error("生成文件链接失败: {}", name, e);
                        urls.add("Error: Link generation failed");
                    }
                }
            }
        }
        dto.setAttachmentUrls(urls);

        return dto;
    }
}