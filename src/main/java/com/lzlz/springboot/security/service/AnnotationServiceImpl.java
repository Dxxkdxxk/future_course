package com.lzlz.springboot.security.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lzlz.springboot.security.domain.AnnotationTypeEnum;
import com.lzlz.springboot.security.entity.Annotation;
import com.lzlz.springboot.security.mapper.AnnotationMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentItemMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentRecordMapper;
import com.lzlz.springboot.security.mapper.SelfAssessmentTaskMapper;
import com.lzlz.springboot.security.response.AnnotationAddRequest;
import com.lzlz.springboot.security.response.AnnotationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnnotationServiceImpl extends ServiceImpl<AnnotationMapper, Annotation> implements AnnotationService {

    @Autowired
    private MinIOService minIOService;

    private final AnnotationMapper annotationMapper;
    // 构造器注入
    public AnnotationServiceImpl(AnnotationMapper annotationMapper) {
        this.annotationMapper = annotationMapper;
    }

    @Override
    public Long addAnnotation(AnnotationAddRequest request, Long id) {
        // 转换为实体
        Annotation annotation = new Annotation();
        BeanUtils.copyProperties(request, annotation);
        annotation.setStudentId(id);

        // 保存
        save(annotation);
        return annotation.getId();
    }

    @Override
    public List<AnnotationResponse> getAnnotationList(Long studentId, Long resourceId) {
        List<Annotation> list = annotationMapper.selectByStudentAndTextbook(studentId, resourceId);
        // 流式转换为响应对象，并处理MinIO临时URL
        return list.stream().map(annotation -> {
            AnnotationResponse resp = new AnnotationResponse();
            // 复制原有属性（id、studentId、comment等）
            BeanUtils.copyProperties(annotation, resp);

            // 3. 关键：获取minioObjectName并判断非空
            String minioObjectName = annotation.getMinioObjectName();
            log.info("minioObjectName:{}", minioObjectName);
            if (StringUtils.hasText(minioObjectName)) {
                try {
                    // 调用getPresignedUrl生成临时URL（传入统一桶名和文件对象名）
                    String presignedFileUrl = minIOService.getPresignedUrl(minioObjectName);
                    // 赋值给返回对象的fileUrl字段
                    resp.setFileUrl(presignedFileUrl);
                } catch (Exception e) {
                    // 可选：给fileUrl赋值默认提示，便于前端处理
                    resp.setFileUrl(null);
                }
            }

            // 返回转换后的响应对象
            return resp;
        }).collect(Collectors.toList());
    }
}
