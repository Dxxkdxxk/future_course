package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lzlz.springboot.security.entity.Chapter;

import java.util.List;

public interface ChapterService extends IService<Chapter> {

    /**
     * 根据教材ID查询章节树形结构
     * @param textbookId 教材ID
     * @return 章节树形列表
     */
    List<Chapter> getChapterTreeByTextbookId(Long textbookId);

    List<Chapter> getChapterTreeByCourseId(Long courseId);
}
