package com.lzlz.springboot.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    @Value("${neo4j.uri}")
    private String uri;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;

    @Bean
    public Driver neo4jDriver() {
        System.out.println("===== Neo4j Config Check =====");
        System.out.println("neo4j.uri = " + uri);
        System.out.println("neo4j.username = " + username);
        System.out.println("neo4j.password.length = " + (password == null ? 0 : password.length()));
        System.out.println("==============================");

        if (uri == null || username == null || password == null) {
            throw new IllegalStateException("在 application.yaml 中找不到 neo4j 的 uri 或 username 或 password 配置！");
        }

        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));

        try {
            driver.verifyConnectivity();
            System.out.println("Neo4j connectivity verified successfully.");
        } catch (Exception e) {
            System.err.println("Neo4j connectivity verification failed: " + e.getMessage());
            throw e;
        }

        return driver;
    }
}
