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
    private CourseMapper courseMapper;

    @Autowired
    private CourseLearningChapterService courseLearningChapterService;

    @Transactional(rollbackFor = Exception.class)
    public void saveResource(Long courseId, Long chapterId, ResourceDto.UploadRequest request) {
        if (courseMapper.selectById(courseId) == null) {
            throw new ResourceNotFoundException("课程不存在: " + courseId);
        }
        if (chapterId == null || chapterId < 0) {
            throw new ResourceNotFoundException("章节ID不合法");
        }
        if (!chapterId.equals(0L) && !courseLearningChapterService.existsInCourse(courseId, chapterId)) {
            throw new ResourceNotFoundException("章节不存在，或不属于当前课程");
        }

        ChapterResource resource = new ChapterResource();
        resource.setCourseId(courseId);
        resource.setChapterId(0L);
        resource.setLearningChapterId(chapterId);
        resource.setFileId(request.getResourceId());
        resource.setResourceName(request.getMaterialName());
        resource.setMaterialType(request.getMaterialType());
        resource.setFileSize(request.getFileSize());
        resource.setIsVideo(request.getIsVedio());
        resource.setIsRequired(request.getIsRequired());
        resource.setSection(request.getSection());
        resource.setDescription(request.getMaterialDescription());
        resourceMapper.insert(resource);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long courseId, Long chapterId, Long resourceId) {
        QueryWrapper<ChapterResource> wrapper = new QueryWrapper<>();
        wrapper.eq("id", resourceId);
        wrapper.eq("course_id", courseId);
        wrapper.eq("learning_chapter_id", chapterId);
        int rows = resourceMapper.delete(wrapper);
        if (rows == 0) {
            throw new ResourceNotFoundException("删除失败：资源不存在，或不属于指定课程/章节");
        }
    }

    public List<ResourceDto.ResourceView> getResources(Long courseId, Long chapterId) {
        if (courseMapper.selectById(courseId) == null) {
            throw new ResourceNotFoundException("获取失败：课程不存在 (ID: " + courseId + ")");
        }
        if (chapterId == null || chapterId < 0) {
            throw new ResourceNotFoundException("章节ID不合法");
        }
        if (!chapterId.equals(0L) && !courseLearningChapterService.existsInCourse(courseId, chapterId)) {
            throw new ResourceNotFoundException("获取失败：章节不存在，或不属于当前课程");
        }

        QueryWrapper<ChapterResource> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.eq("learning_chapter_id", chapterId);
        wrapper.orderByDesc("created_at");
        List<ChapterResource> list = resourceMapper.selectList(wrapper);

        return list.stream().map(r -> {
            ResourceDto.ResourceView view = new ResourceDto.ResourceView();
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
