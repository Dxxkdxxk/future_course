package com.lzlz.springboot.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pkslow.jwt")
public class JwtProperties {
    private String secretKey = "pkslow.key";
    private long validityInMs = 100000_000;
}
