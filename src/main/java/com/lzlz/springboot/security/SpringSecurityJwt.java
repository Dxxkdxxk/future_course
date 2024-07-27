package com.lzlz.springboot.security;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lzlz.springboot.security.mapper")
public class SpringSecurityJwt {
    public static void main(String[] args) {
        SpringApplication.run(SpringSecurityJwt.class, args);
    }
}
