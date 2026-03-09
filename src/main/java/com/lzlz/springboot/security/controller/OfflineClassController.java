package com.lzlz.springboot.security.controller;


import com.github.pagehelper.PageInfo;
import com.lzlz.springboot.security.entity.ClassDiscuss;
import com.lzlz.springboot.security.entity.SignStatisticsVO;
import com.lzlz.springboot.security.entity.SignTaskVO;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.response.Result;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.service.ClassDiscussService;
import com.lzlz.springboot.security.service.SignTaskService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/offline_class")
@CrossOrigin // 允许跨域
public class OfflineClassController {

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private final ClassDiscussService classDiscussService;
    private final SignTaskService signTaskService;

    // 构造器注入
    @Autowired
    public OfflineClassController(ClassDiscussService classDiscussService, SignTaskService signTaskService) {
        this.classDiscussService = classDiscussService;
        this.signTaskService = signTaskService;
    }

    // ====================== 课堂讨论接口 ======================
    @PostMapping("/discuss/publish")
    public Result<Void> publishDiscuss(
            @RequestBody ClassDiscussRequest request,
            HttpServletRequest httpRequest
    ) {
        // 提取请求头用户信息
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        return classDiscussService.publishDiscuss(Long.valueOf(user.getId()), user.getRole(), request.getCourseId(), request.getContent());
    }

    @GetMapping("/discuss/list")
    public Result<PageInfo<ClassDiscuss>> getDiscussList(
            @RequestParam Long courseId,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize
    ) {
        return classDiscussService.getDiscussList(courseId, pageNum, pageSize);
    }

    // ====================== 签到发布接口（教师） ======================
    @PostMapping("/sign/publish")
    public Result<SignTaskVO> publishSignTask(
            @RequestBody SignTaskPublishRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        return signTaskService.publishSignTask(Long.valueOf(user.getId()), request.getCourseId(), request.getTaskTitle(), request.getValidDuration());
    }

    // ====================== 学生签到接口 ======================
    @PostMapping("/sign/student/submit")
    public Result<Void> studentSubmitSign(
            @RequestBody StudentSignRequest request,
            HttpServletRequest httpRequest
    ) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        // 获取客户端IP（简化实现）
        String signIp = httpRequest.getRemoteAddr();
        return signTaskService.studentSubmitSign(Long.valueOf(user.getId()), request.getSignTaskId(), signIp);
    }

    // ====================== 签到统计接口（教师） ======================
    @GetMapping("/sign/statistics/detail")
    public Result<SignStatisticsVO> getSignDetailStatistics(
            @RequestParam Long signTaskId,
            HttpServletRequest httpRequest
    ) {
        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        User user = (User) userDetailsService.loadUserByUsername(username);
        return signTaskService.getSignDetailStatistics(Long.valueOf(user.getId()), signTaskId);
    }

    // 辅助请求DTO（封装请求参数）
    @Data
    static class ClassDiscussRequest {
        private Long courseId;
        private String content;
    }

    @Data
    static class SignTaskPublishRequest {
        private Long courseId;
        private String taskTitle;
        private Integer validDuration;
    }

    @Data
    static class StudentSignRequest {
        private Long signTaskId;
    }
}
