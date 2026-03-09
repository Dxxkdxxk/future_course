package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.dto.ResourceDto;
import com.lzlz.springboot.security.entity.ChapterResource;
import com.lzlz.springboot.security.exception.ResourceNotFoundException;
import com.lzlz.springboot.security.mapper.ChapterResourceMapper;
import com.lzlz.springboot.security.mapper.CourseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChapterResourceService {

    @Autowired
    private ChapterResourceMapper resourceMapper;

    @Autowired
    private CourseMapper courseMapper; // 只保留课程检查

    // (!!!) 移除了 ChapterMapper

    /**
     * 上传/保存资源
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveResource(Long courseId, Long chapterId, ResourceDto.UploadRequest request) {

        // 1. (!!!) 校验课程是否存在
        // 即使不校验章节，校验课程也是一种基本的安全保障
        if (courseMapper.selectById(courseId) == null) {
            throw new ResourceNotFoundException("课程不存在: " + courseId);
        }

        // 2. (!!!) 章节校验已移除
        // 我们假设前端传来的 chapterId 是对的，直接存入

        // 3. 执行保存
        ChapterResource resource = new ChapterResource();
        resource.setCourseId(courseId);
        resource.setChapterId(chapterId);
        resource.setFileId(request.getResourceId());
        resource.setResourceName(request.getMaterialName());
        resource.setMaterialType(request.getMaterialType()); // 记得我们加过这个字段
        resource.setFileSize(request.getFileSize());
        resource.setIsVideo(request.getIsVedio());
        resource.setIsRequired(request.getIsRequired());
        resource.setSection(request.getSection());
        resource.setDescription(request.getMaterialDescription());

        resourceMapper.insert(resource);
    }

    /**
     * 删除资源 (通过行数检查修复假成功问题)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long courseId, Long chapterId, Long resourceId) {
        // 构造条件：ID匹配 + 课程匹配 + 章节匹配 (防止越权)
        QueryWrapper<ChapterResource> wrapper = new QueryWrapper<>();
        wrapper.eq("id", resourceId);
        wrapper.eq("course_id", courseId);
        wrapper.eq("chapter_id", chapterId);

        // (!!!) 关键修复：获取删除操作的影响行数
        int rows = resourceMapper.delete(wrapper);

        // (!!!) 如果行数为0，说明资源不存在，抛出异常
        if (rows == 0) {
            throw new ResourceNotFoundException("删除失败：资源不存在，或不属于指定课程/章节");
        }
    }

    /**
     * 获取资源列表
     */
    public List<ResourceDto.ResourceView> getResources(Long courseId, Long chapterId) {
        if (courseMapper.selectById(courseId) == null) {
            throw new ResourceNotFoundException("获取失败：课程不存在 (ID: " + courseId + ")");
        }

        // 2. 校验章节逻辑 (根据你目前的架构情况)
        if (chapterId != null && !chapterId.equals(0L)) {
            // TODO: 如果未来你恢复了 ChapterMapper，请在这里解开注释
            /*
            Chapter chapter = chapterMapper.selectById(chapterId);
            if (chapter == null) {
                throw new ResourceNotFoundException("获取失败：章节不存在");
            }
            // 更高级的安全检查：确认该章节真的属于这个课程 (防止 ID 越权)
            if (!chapter.getCourseId().equals(courseId)) {
                throw new ResourceNotFoundException("数据异常：该章节不属于指定课程");
            }
            */
        }
        QueryWrapper<ChapterResource> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.eq("chapter_id", chapterId);
        wrapper.orderByDesc("created_at");

        List<ChapterResource> list = resourceMapper.selectList(wrapper);

        return list.stream().map(r -> {
            ResourceDto.ResourceView view = new ResourceDto.ResourceView();

            // (!!!) 修改点：把实体的 fileId 赋值给 DTO 的 resourceId
            view.setResourceId(r.getFileId());
            view.setResourceName(r.getResourceName());
            view.setMaterialType(r.getMaterialType());
            view.setFileSize(r.getFileSize());
            view.setIsVideo(r.getIsVideo());
            view.setIsRequired(r.getIsRequired());
            view.setSection(r.getSection());
            view.setDescription(r.getDescription());
            return view;
        }).collect(Collectors.toList());
    }
}