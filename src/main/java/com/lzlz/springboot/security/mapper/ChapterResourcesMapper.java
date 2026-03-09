package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lzlz.springboot.security.entity.ChapterResources;
import com.lzlz.springboot.security.entity.ResourceStatisticDTO;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 章节资源关联表Mapper接口
 */
@Mapper
public interface ChapterResourcesMapper extends BaseMapper<ChapterResources> {

    /**
     * 按courseId分组统计资源数量（按文件类型）
     */
    @Select("SELECT cr.course_id as groupId, t.file_type as fileType, COUNT(*) as count " +
            "FROM chapter_resources cr " +
            "LEFT JOIN textbook t ON cr.resource_id = t.id " +
            "WHERE cr.course_id = #{courseId} " +
            "GROUP BY cr.course_id, t.file_type")
    List<ResourceStatisticDTO.FileTypeCountDTO> countByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询指定courseId的所有资源详情（关联课程/章节/资源表）
     */
    @Select("SELECT t.id, t.name, t.file_type, t.file_size, t.create_time, t.uploader_id, t.minio_object_name" +
            "cr.course_id, cr.chapter_id " +
            "FROM chapter_resources cr " +
            "LEFT JOIN textbook t ON cr.resource_id = t.id " +
            "WHERE cr.course_id = #{courseId} " +
            "ORDER BY t.create_time DESC")
    List<ResourceStatisticDTO.ResourceDetailDTO> listByCourseId(@Param("courseId") Long courseId);
}
