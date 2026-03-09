package com.lzlz.springboot.security;

import com.lzlz.springboot.security.config.MinIOConfig;
import com.lzlz.springboot.security.config.WebSocketConfig;
import com.lzlz.springboot.security.service.MinIOService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
@TestConfiguration
@ComponentScan(
        basePackages = "com.lzlz.springboot.security", // 你的业务包
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebSocketConfig.class // 排除WebSocket配置类
                )
        }
)
@ActiveProfiles("test") // 激活test环境，WebSocketConfig自动排除
public class MinIOTest {
    @Autowired
    private MinIOService minIOService;

    @Autowired
    private MinioClient minIOClient;

    @Autowired
    private MinIOConfig minIOConfig;

    private static MultipartFile file = new MockMultipartFile("test.txt",
            "test.txt",
            "text/plain",
            "test upload".getBytes());

    @BeforeEach
    void setUp() {
        assertSame(minIOConfig.minioClient(), minIOClient, "minIO实例不一致");
        log.info("minIO实例准备完成，服务地址:{}", minIOConfig.endpoint);
    }
    @Test
    void uploadFileTest() throws Exception {

        String url = minIOService.getPresignedUrl("781206e2-6945-4707-a467-972f1d3d588a.mp4");
        log.info("获取url成功，url为:{}", url);
    }
}
