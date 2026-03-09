package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.config.FileSizeUtil;
import com.lzlz.springboot.security.entity.ResourceStatisticDTO;
import com.lzlz.springboot.security.mapper.ChapterResourcesMapper;
import com.lzlz.springboot.security.mapper.TextbookMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ResourceStatisticService {

    @Resource
    private TextbookMapper textbookMapper;
    @Resource
    private ChapterResourcesMapper chapterResourcesMapper;

    @Resource
    private MinIOService minioUtil;
    /**
     * 按上传人ID统计资源
     */
    public ResourceStatisticDTO statisticByUploaderId(Long uploaderId) {
        // 1. 按文件类型统计
        List<ResourceStatisticDTO.FileTypeCountDTO> typeCountList = textbookMapper.countByUploaderId(uploaderId);
        // 2. 计算总数
        int totalCount = typeCountList.stream().mapToInt(ResourceStatisticDTO.FileTypeCountDTO::getCount).sum();
        // 3. 查询资源详情并格式化文件大小
        List<ResourceStatisticDTO.ResourceDetailDTO> detailList = textbookMapper.listByUploaderId(uploaderId);
        detailList.forEach(detail -> {
            // 格式化文件大小
            detail.setFileSize(FileSizeUtil.formatFileSize(detail.getFileSizeBytes()));
            // 生成MinIO临时链接
            String presignedUrl = null;
            try {
                presignedUrl = minioUtil.getPresignedUrl(
                        detail.getMinioObjectName()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            detail.setPresignedUrl(presignedUrl); // 赋值到DTO
        });

        // 组装返回DTO
        ResourceStatisticDTO result = new ResourceStatisticDTO();
        result.setGroupId(uploaderId);
        result.setTotalCount(totalCount);
        result.setTypeCountList(typeCountList);
        result.setResourceDetailList(detailList);
        return result;
    }

    /**
     * 按课程ID统计资源
     */
    public ResourceStatisticDTO statisticByCourseId(Long courseId) {
        // 1. 按文件类型统计
        List<ResourceStatisticDTO.FileTypeCountDTO> typeCountList = chapterResourcesMapper.countByCourseId(courseId);
        // 2. 计算总数
        int totalCount = typeCountList.stream().mapToInt(ResourceStatisticDTO.FileTypeCountDTO::getCount).sum();
        // 3. 查询资源详情并格式化文件大小
        List<ResourceStatisticDTO.ResourceDetailDTO> detailList = chapterResourcesMapper.listByCourseId(courseId);
        detailList.forEach(detail -> {
            // 格式化文件大小
            detail.setFileSize(FileSizeUtil.formatFileSize(detail.getFileSizeBytes()));
            // 生成MinIO临时链接
            String presignedUrl = null;
            try {
                presignedUrl = minioUtil.getPresignedUrl(
                        detail.getMinioObjectName()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            detail.setPresignedUrl(presignedUrl); // 赋值到DTO
        });

        // 组装返回DTO
        ResourceStatisticDTO result = new ResourceStatisticDTO();
        result.setGroupId(courseId);
        result.setTotalCount(totalCount);
        result.setTypeCountList(typeCountList);
        result.setResourceDetailList(detailList);
        return result;
    }
}
