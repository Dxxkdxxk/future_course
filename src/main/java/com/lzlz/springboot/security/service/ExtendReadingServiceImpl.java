package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.domain.ExtendReadingMaterialTypeEnum;
import com.lzlz.springboot.security.entity.ExtendReading;
import com.lzlz.springboot.security.mapper.ExtendReadingMapper;
import com.lzlz.springboot.security.response.ExtendReadingAddRequest;
import com.lzlz.springboot.security.response.ExtendReadingResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExtendReadingServiceImpl extends ServiceImpl<ExtendReadingMapper, ExtendReading> implements ExtendReadingService {

    @Override
    public Long addExtendReading(ExtendReadingAddRequest request) {
        // 校验材料类型
        ExtendReadingMaterialTypeEnum typeEnum = ExtendReadingMaterialTypeEnum.getByCode(request.getMaterialType());

        // 按类型校验必填字段
        switch (typeEnum) {
            case URL:
                if (!StringUtils.hasText(request.getUrl())) {
                    throw new IllegalArgumentException("网页链接类型必须填写URL");
                }
                break;
            case TEXT:
                if (!StringUtils.hasText(request.getContent())) {
                    throw new IllegalArgumentException("文本类型必须填写内容");
                }
                break;
            case FILE:
                if (!StringUtils.hasText(request.getMinioBucket()) || !StringUtils.hasText(request.getMinioObjectName())) {
                    throw new IllegalArgumentException("文件类型必须填写MinIO桶名和对象名");
                }
                if (request.getFileSize() == null || !StringUtils.hasText(request.getFileType())) {
                    throw new IllegalArgumentException("文件类型必须填写文件大小和类型");
                }
                break;
            default:
                throw new IllegalArgumentException("不支持的材料类型");
        }

        // 转换为实体
        ExtendReading reading = new ExtendReading();
        BeanUtils.copyProperties(request, reading);
        reading.setPosition(request.getPositionInfo());

        // 保存
        save(reading);
        return reading.getId();
    }

    @Override
    public List<ExtendReadingResponse> getExtendReadingList(String studentId, Long textbookId, Long chapterId) {
        List<ExtendReading> list = baseMapper.selectByStudentAndTextbookAndChapter(studentId, textbookId, chapterId);
        return list.stream().map(reading -> {
            ExtendReadingResponse resp = new ExtendReadingResponse();
            BeanUtils.copyProperties(reading, resp);
            resp.setPositionInfo(reading.getPosition());
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public boolean deleteExtendReading(Long id, String studentId) {
        // 校验归属权
        LambdaQueryWrapper<ExtendReading> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExtendReading::getId, id).eq(ExtendReading::getStudentId, studentId);
        if (getOne(wrapper) == null) {
            throw new IllegalArgumentException("拓展阅读不存在或无权删除");
        }
        // 如需删除MinIO文件，可在此调用MinIO工具类
        return removeById(id);
    }
}
