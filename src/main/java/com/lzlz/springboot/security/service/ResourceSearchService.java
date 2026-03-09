package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lzlz.springboot.security.config.FileSizeUtil;
import com.lzlz.springboot.security.entity.ResourceSearchDTO;
import com.lzlz.springboot.security.entity.ResourceSearchResultDTO;
import com.lzlz.springboot.security.mapper.TextbookMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 资源检索服务
 */
@Service
public class ResourceSearchService {

    @Resource
    private TextbookMapper textbookMapper;

    /**
     * 模糊检索资源（按文件标题）
     * @param dto 检索条件
     * @return 分页结果
     */
    public ResourceSearchResultDTO searchResources(ResourceSearchDTO dto) {
        // 1. 初始化分页参数
        Page<ResourceSearchResultDTO.ResourceDetailDTO> page = new Page<>(
                dto.getPageNum(), dto.getPageSize()
        );

        // 2. 执行模糊查询
        Page<ResourceSearchResultDTO.ResourceDetailDTO> resultPage =
                (Page<ResourceSearchResultDTO.ResourceDetailDTO>) textbookMapper.searchResources(page, dto);

        // 3. 格式化文件大小
        resultPage.getRecords().forEach(detail -> {
            detail.setFileSize(FileSizeUtil.formatFileSize(detail.getFileSizeBytes()));
        });

        // 4. 组装返回结果
        ResourceSearchResultDTO result = new ResourceSearchResultDTO();
        result.setTotal(resultPage.getTotal());
        result.setPages((int) resultPage.getPages());
        result.setRecords(resultPage.getRecords());
        return result;
    }
}