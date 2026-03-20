package com.lzlz.springboot.security.service;

import org.springframework.beans.factory.annotation.Qualifier;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@Data
@Slf4j
public class MinIOService {
    @Autowired
    @Qualifier("publicMinioClient")
    private MinioClient publicMinioClient;

    @Autowired
    @Qualifier("innerMinioClient")
    private MinioClient innerMinioClient;

    @Value("${minio.bucket}")
    private String minioBucket;

    /**
     * 上传文件到MinIO
     * 返回文件名
     */
    public String uploadFile(MultipartFile file) throws Exception {
        if (!innerMinioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build()))  {
            innerMinioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
        }
        log.info("正在上传文件:{}", file.getOriginalFilename());
        log.info(">>> uploadFile 使用 innerMinioClient 开始上传");
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = UUID.randomUUID() + suffix;

        innerMinioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioBucket)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .object(objectName)
                .build());

        log.info("文件上传成功, bucket:{}, object:{}", minioBucket, objectName);
        return objectName;
    }

    public String getPresignedUrl(String objectName) throws Exception{
        return publicMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .bucket(minioBucket)
                        .method(Method.GET)
                        .object(objectName)
                        .expiry(3600)
                .build());
    }
}
