package com.lzlz.springboot.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ... imports

@Configuration
public class Neo4jConfig {

    // 必须是 @Value("${neo4j.uri}")，注意花括号和$符号
    @Value("${neo4j.uri}")
    private String uri;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;

    @Bean
    public Driver neo4jDriver() {
        if (uri == null || username == null || password == null) {
            throw new IllegalStateException("在 application.properties 中找不到 neo4j 的 uri 或 username 或 password 配置！");
        }
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
}