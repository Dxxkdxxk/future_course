package com.lzlz.springboot.security.controller;

import cn.hutool.core.io.resource.InputStreamResource;
import com.lzlz.springboot.security.entity.*;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.mapper.CourseTextbookRelationMapper;
import com.lzlz.springboot.security.mapper.TextbookMapper;
import com.lzlz.springboot.security.response.TextbookApiResponse;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.service.*;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/textbook")
@Slf4j
public class TextbookController {
    @Autowired
    private MinIOService minIOService;
    @Autowired
    private TextbookMapper textbookMapper;

    @Autowired
    private CourseTextbookRelationMapper courseTextbookRelationMapper;

    @Autowired
    private DocumentParseService documentParseService;

    @Resource
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Resource
    private ResourceStatisticService resourceStatisticService;

    private static final Map<String, String> SUFFIX_TYPE_MAP = new HashMap<>();

    // 静态初始化映射表
    static {
        // 文档类
        SUFFIX_TYPE_MAP.put("pdf", "PDF");
        SUFFIX_TYPE_MAP.put("doc", "WORD");
        SUFFIX_TYPE_MAP.put("docx", "WORD");
        SUFFIX_TYPE_MAP.put("txt", "TXT");
        SUFFIX_TYPE_MAP.put("xls", "EXCEL");
        SUFFIX_TYPE_MAP.put("xlsx", "EXCEL");
        // 图片类
        SUFFIX_TYPE_MAP.put("jpg", "IMAGE");
        SUFFIX_TYPE_MAP.put("jpeg", "IMAGE");
        SUFFIX_TYPE_MAP.put("png", "IMAGE");
        // 视频类
        SUFFIX_TYPE_MAP.put("mp4", "VIDEO");
        SUFFIX_TYPE_MAP.put("avi", "VIDEO");
    }

    @Resource
    private ChapterService chapterService;
    /**
     * 教材上传接口（前端调用）
     */
    @PostMapping("/upload")
    public Result<?> uploadTextbook(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("isTextbook") boolean isTextbook,
            @RequestParam("courseId") Long courseId) {

        try {
            String token = jwtTokenProvider.resolveToken(request);
            String username = jwtTokenProvider.getUsername(token);
            User user = (User) userDetailsService.loadUserByUsername(username);
//            // 1. 文件合法性校验（类型+大小）
            String originalFilename = file.getOriginalFilename();
//            if (!originalFilename.endsWith(".pdf") && !originalFilename.endsWith(".docx")) {
//                return Result.fail("仅支持PDF和docx格式文件");
//            }
            if (file.getSize() > 1024 * 1024 * 100) { // 限制100MB
                return Result.fail("文件大小不能超过100MB");
            }

            // 2. 上传文件到MinIO
            String minioObjectName = minIOService.uploadFile(file);

            // 3. 保存教材基础信息到MySQL（状态：PENDING）
            Textbook textbook = new Textbook();
            textbook.setName(name);
            textbook.setFileType(SUFFIX_TYPE_MAP.getOrDefault(originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase(), "OTHER"));
            textbook.setMinioBucket(minIOService.getMinioBucket()); // 需在MinIOService中添加getter
            textbook.setMinioObjectName(minioObjectName);
            textbook.setFileSize(file.getSize());
            textbook.setUploaderId(Long.valueOf(user.getId()));
            textbook.setStatus(Textbook.STATUS_PENDING);
            textbookMapper.insert(textbook);

            if (courseId != null) {
                CourseTextbookRelation relation = new CourseTextbookRelation();
                relation.setCourseId(courseId);
                relation.setTextbookId(textbook.getId());
                courseTextbookRelationMapper.insert(relation);
            }

            log.info("传输了isTextbook的值，为{}", isTextbook);
            // 4. 异步触发文档分节（不阻塞当前请求）
            if (isTextbook) {
                log.info("开始解析");
                documentParseService.asyncParseChapter(textbook.getId());
                log.info("解析完成");
            }

            // 5. 生成教材访问URL（前端阅读用）
            String accessUrl = minIOService.getPresignedUrl(minioObjectName);

            // 6. 即时响应前端：存储完成
            return Result.success("教材存储完成，分节中...", new HashMap<String, Object>() {{
                put("textbookId", textbook.getId());
                put("textbookName", name);
                put("url", accessUrl);
                put("status", Textbook.STATUS_PENDING);
            }});
        } catch (Exception e) {
            log.error("教材上传失败", e);
            return Result.fail("教材上传失败：" + e.getMessage());
        }
    }


    /**
     * 接口1：按上传人ID统计资源（从请求头解析用户ID）
     *
     */
    @GetMapping("/statistic/by-uploader")
    public ResourceStatisticDTO statisticByUploader(
            // 假设请求头Authorization格式为 Bearer <userId>（实际项目中需解析JWT获取userId）
            HttpServletRequest request) {

        // 解析Authorization头：去除Bearer前缀，获取用户ID
        Long uploaderId = null;
        try {
            String token = jwtTokenProvider.resolveToken(request);
            String username = jwtTokenProvider.getUsername(token);
            User user = (User) userDetailsService.loadUserByUsername(username);
            uploaderId = Long.valueOf(user.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的Authorization令牌，无法解析用户ID");
        }

        return resourceStatisticService.statisticByUploaderId(uploaderId);
    }
    /**
     * 根据教材ID查询章节树形结构
     * @param courseId 教材ID
     * @return 章节树形列表响应
     */
    @GetMapping("/{courseId}/by-course/chapters")
    public TextbookApiResponse<List<Chapter>> getChaptersByCourseId(@PathVariable Long courseId) {
        List<Chapter> chapterTree = chapterService.getChapterTreeByCourseId(courseId);
        return TextbookApiResponse.success(chapterTree);
    }

    @GetMapping("/{textbookId}/by-textbook/chapters")
    public TextbookApiResponse<List<Chapter>> getChaptersByTextbookId(@PathVariable Long textbookId) {
        List<Chapter> chapterTree = chapterService.getChapterTreeByTextbookId(textbookId);
        return TextbookApiResponse.success(chapterTree);
    }
    /**
     * 接口2：按课程ID统计资源
     * @param courseId 课程ID（路径参数）
     */
    @GetMapping("/statistic/by-course/{courseId}")
    public ResourceStatisticDTO statisticByCourse(@PathVariable Long courseId) {
        if (courseId == null || courseId <= 0) {
            throw new IllegalArgumentException("课程ID不能为空且必须为正数");
        }
        return resourceStatisticService.statisticByCourseId(courseId);
    }

    @GetMapping("/{resourceId}/preview")
    public Result<?> getUrl(@PathVariable Long resourceId) {
        try {
            Textbook textbook = textbookMapper.selectById(resourceId);
            return Result.success("教材临时链接获取成功", new HashMap<String, String>() {{
                put("url", minIOService.getPresignedUrl(textbook.getMinioObjectName()));
            }});
        } catch (Exception e) {
            log.error("获取临时链接失败", e);
            return Result.fail("获取临时链接失败：" + e.getMessage());
        }
    }

    @Resource
    private ResourceSearchService resourceSearchService;

    /**
     * 模糊检索资源（按文件标题）
     * @param dto 检索条件（文件标题、上传人、文件类型、分页）
     * @return 分页检索结果
     */
    @PostMapping("/search/by-name")
    public ResourceSearchResultDTO searchByName(@Valid @RequestBody ResourceSearchDTO dto) {
        // 入参校验（非空/格式）由@Valid + 注解完成（可扩展DTO校验）
        return resourceSearchService.searchResources(dto);
    }

    // 辅助类：统一返回结果
    @Data
    public static class Result<T> {
        private int code;
        private String msg;
        private T data;

        public static <T> Result<T> success(String msg, T data) {
            Result<T> result = new Result<>();
            result.setCode(200);
            result.setMsg(msg);
            result.setData(data);
            return result;
        }

        public static <T> Result<T> fail(String msg) {
            Result<T> result = new Result<>();
            result.setCode(500);
            result.setMsg(msg);
            result.setData(null);
            return result;
        }
    }

}
