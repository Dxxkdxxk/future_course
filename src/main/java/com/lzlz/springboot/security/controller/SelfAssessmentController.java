package com.lzlz.springboot.security.controller;


import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.service.SelfAssessmentStudentService;
import com.lzlz.springboot.security.service.SelfAssessmentTaskService;

import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/self-assessment")
@CrossOrigin // 允许跨域
public class SelfAssessmentController {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    private final SelfAssessmentTaskService selfAssessmentTaskService;
    private final SelfAssessmentStudentService selfAssessmentStudentService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    // 构造器注入
    public SelfAssessmentController(SelfAssessmentTaskService selfAssessmentTaskService, SelfAssessmentStudentService selfAssessmentStudentService) {
        this.selfAssessmentTaskService = selfAssessmentTaskService;
        this.selfAssessmentStudentService = selfAssessmentStudentService;
    }

    // ====================== 教师自评发布接口 ======================
    @PostMapping("/publish")
    public Result<SelfAssessmentTask> publishTask(
            @RequestBody AssessmentTaskPublishVO publishVO,
            HttpServletRequest httpRequest
    ) {
        // 提取教师信息，校验用户类型
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        Long teacherId = Long.valueOf(user.getId());
        return selfAssessmentTaskService.publishTask(teacherId, publishVO);
    }

    @GetMapping("/task/list")
    public Result<PageInfo<SelfAssessmentTask>> getTeacherTaskList(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        Long teacherId = Long.valueOf(user.getId());
        return selfAssessmentTaskService.getTeacherTaskList(teacherId, pageNum, pageSize);
    }

    // ====================== 学生自评接口 ======================
    @PostMapping("/submit")
    public Result<Void> submitAssessment(
            @RequestBody StudentAssessmentSubmitVO submitVO,
            HttpServletRequest httpRequest
    ) {
        // 提取学生信息，校验用户类型
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        Long studentId = Long.valueOf(user.getId());
        return selfAssessmentStudentService.submitAssessment(studentId, submitVO);
    }

    @GetMapping("/student/task/list")
    public Result<PageInfo<SelfAssessmentTask>> getStudentValidTaskList(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        Long studentId = Long.valueOf(user.getId());
        return selfAssessmentStudentService.getValidTaskList(studentId, pageNum, pageSize);
    }

    @GetMapping("/student/task/detail")
    public Result<List<SelfAssessmentItem>> getStudentValidTaskDetail(
            @RequestParam Long taskId
    ) {
        return selfAssessmentStudentService.getTaskDetail(taskId);
    }

    // 新增1：/record/list 接口 - 分页查询学生自评记录
    @GetMapping("/record/list")
    public Result<PageInfo<StudentRecordVO>> getStudentRecordList(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        Long studentId = Long.valueOf(user.getId());
        return selfAssessmentTaskService.getStudentRecordList(studentId, pageNum, pageSize);
    }

    // 新增2：/task/detail 接口 - 查询自评任务详情
    @GetMapping("/task/detail")
    public Result<AssessmentTaskDetailVO> getTaskDetail(@RequestParam Long taskId,
    @RequestParam Long studentId) {
        return selfAssessmentTaskService.getTaskDetail(taskId, studentId);
    }
}
