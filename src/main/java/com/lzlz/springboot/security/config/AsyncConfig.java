package com.lzlz.springboot.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync // 启用 Spring 的 @Async 异步方法执行功能
public class AsyncConfig {
    // 这里可以配置线程池，但为了简单起见，我们先使用默认配置
}