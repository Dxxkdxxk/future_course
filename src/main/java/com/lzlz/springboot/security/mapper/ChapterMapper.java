package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.Chapter;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {
    @Insert("<script>" +
            "INSERT INTO chapter (textbook_id, parent_id, level, title, sort, pdf_page, content, create_time) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','> " +
            "(#{item.textbookId}, #{item.parentId}, #{item.level}, #{item.title}, #{item.sort}, #{item.pdfPage}, #{item.content}, #{item.createTime}) " +
            "</foreach> " +
            "</script>")
    int batchInsert(@Param("list") List<Chapter> chapters);
}