package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.entity.Chapter;
import com.lzlz.springboot.security.entity.CourseTextbookRelation;
import com.lzlz.springboot.security.mapper.ChapterMapper;
import com.lzlz.springboot.security.mapper.CourseTextbookRelationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    @Resource
    private CourseTextbookRelationMapper courseTextbookRelationMapper;

    @Override
    public List<Chapter> getChapterTreeByTextbookId(Long textbookId) {
        // 1. 查询该教材下所有章节（按sort升序排列）
        LambdaQueryWrapper<Chapter> queryWrapper = new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getTextbookId, textbookId)
                .orderByAsc(Chapter::getSort);
        List<Chapter> allChapters = baseMapper.selectList(queryWrapper);

        // 2. 构建ID->Chapter的映射，方便快速查找父节点
        Map<Long, Chapter> chapterMap = new HashMap<>();
        for (Chapter chapter : allChapters) {
            chapter.setChildren(new ArrayList<>()); // 初始化子节点列表
            chapterMap.put(chapter.getId(), chapter);
        }

        // 3. 构建树形结构
        List<Chapter> rootChapters = new ArrayList<>();
        for (Chapter chapter : allChapters) {
            Long parentId = chapter.getParentId();
            if (parentId == null) {
                // 父ID为空，是顶级章节
                rootChapters.add(chapter);
            } else {
                // 找到父章节，将当前章节加入父章节的子节点
                Chapter parentChapter = chapterMap.get(parentId);
                if (parentChapter != null) {
                    parentChapter.getChildren().add(chapter);
                }
            }
        }
        return rootChapters;
    }

    public List<Chapter> getChapterTreeByCourseId(Long courseId) {
        LambdaQueryWrapper<CourseTextbookRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseTextbookRelation::getCourseId, courseId)
                .orderByDesc(CourseTextbookRelation::getCreatedAt)
                .last("LIMIT 1");

        CourseTextbookRelation relation = courseTextbookRelationMapper.selectOne(wrapper);

        if (relation == null) {
            throw new RuntimeException("该课程尚未绑定教材");
        }

        Long textbookId = relation.getTextbookId();
        return getChapterTreeByTextbookId(textbookId);
    }
}
