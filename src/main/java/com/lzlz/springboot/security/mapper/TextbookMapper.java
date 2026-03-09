package com.lzlz.springboot.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lzlz.springboot.security.entity.ResourceSearchDTO;
import com.lzlz.springboot.security.entity.ResourceSearchResultDTO;
import com.lzlz.springboot.security.entity.ResourceStatisticDTO;
import com.lzlz.springboot.security.entity.Textbook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.jmx.export.annotation.ManagedNotification;

import java.util.List;

@Mapper
public interface TextbookMapper extends BaseMapper<Textbook> {

    /**
     * 按uploaderId分组统计资源数量（按文件类型）
     */
    @Select("SELECT uploader_id as groupId, file_type as fileType, COUNT(*) as count " +
            "FROM textbook " +
            "WHERE uploader_id = #{uploaderId} " +
            "GROUP BY uploader_id, file_type")
    List<ResourceStatisticDTO.FileTypeCountDTO> countByUploaderId(@Param("uploaderId") Long uploaderId);

    /**
     * 查询指定uploaderId的所有资源详情
     */
    @Select("SELECT id, name, file_type, file_size, create_time, uploader_id, minio_object_name " +
            "FROM textbook WHERE uploader_id = #{uploaderId} ORDER BY create_time DESC")
    List<ResourceStatisticDTO.ResourceDetailDTO> listByUploaderId(@Param("uploaderId") Long uploaderId);

    /**
     * 模糊检索资源（关联课程/章节信息）
     * @param page 分页参数
     * @param dto 检索条件
     * @return 分页结果
     */
    @Select("<script>" +
            "SELECT t.id, t.name, t.file_type, t.file_size, t.create_time, t.uploader_id, " +
            "t.minio_bucket, t.minio_object_name, t.status, cr.course_id, cr.chapter_id " +
            "FROM textbook t " +
            "LEFT JOIN chapter_resources cr ON t.id = cr.resource_id " +
            "WHERE 1=1 " +
            "<if test='dto.fileName != null and dto.fileName != \"\"'>" +
            "  AND t.name LIKE CONCAT('%', #{dto.fileName}, '%') " +
            "</if>" +
            "<if test='dto.uploaderId != null'>" +
            "  AND t.uploader_id = #{dto.uploaderId} " +
            "</if>" +
            "<if test='dto.fileType != null and dto.fileType != \"\"'>" +
            "  AND t.file_type = #{dto.fileType} " +
            "</if>" +
            "ORDER BY t.create_time DESC" +
            "</script>")
    IPage<ResourceSearchResultDTO.ResourceDetailDTO> searchResources(
            Page<ResourceSearchResultDTO.ResourceDetailDTO> page,
            @Param("dto") ResourceSearchDTO dto);
}
