package com.lzlz.springboot.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sso")
public class SsoProperties {

    private boolean enabled = true;

    private String serverUrl;

    private String loginUrl;

    private String logoutUrl;

    private String validateUrl;

    private String serviceUrl;

    private String frontendCallbackUrl;

    private boolean autoCreateUser = false;

    private int loginCodeTtlSeconds = 120;
}
