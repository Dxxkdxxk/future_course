package com.lzlz.springboot.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.config.SsoProperties;
import com.lzlz.springboot.security.dto.SsoLoginResult;
import com.lzlz.springboot.security.dto.SsoUserInfo;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.exception.SsoAuthenticationException;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class SsoLoginService {

    private static final String LOGIN_CODE_PREFIX = "sso:login-code:";

    private final SsoClientService ssoClientService;
    private final SsoProperties ssoProperties;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisCacheService redisCacheService;

    public SsoLoginService(SsoClientService ssoClientService,
                           SsoProperties ssoProperties,
                           UserMapper userMapper,
                           JwtTokenProvider jwtTokenProvider,
                           RedisCacheService redisCacheService) {
        this.ssoClientService = ssoClientService;
        this.ssoProperties = ssoProperties;
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisCacheService = redisCacheService;
    }

    public String buildLoginUrl() {
        return ssoClientService.buildLoginUrl();
    }

    public String loginByTicketAndCreateCode(String ticket) {
        SsoUserInfo ssoUserInfo = ssoClientService.validateTicket(ticket);
        User user = findLocalUser(ssoUserInfo);
        String token = jwtTokenProvider.createToken(user.getUsername(), List.of(user.getRole()));

        SsoLoginResult result = new SsoLoginResult(
                user.getId(),
                user.getUsername(),
                ssoUserInfo.getName(),
                ssoUserInfo.getEmployeeNumber(),
                user.getRole(),
                token
        );

        String code = UUID.randomUUID().toString().replace("-", "");
        redisCacheService.set(
                LOGIN_CODE_PREFIX + code,
                result,
                Duration.ofSeconds(Math.max(30, ssoProperties.getLoginCodeTtlSeconds()))
        );
        return code;
    }

    public SsoLoginResult exchangeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new SsoAuthenticationException("SSO login code is required");
        }

        String key = LOGIN_CODE_PREFIX + code.trim();
        SsoLoginResult result = redisCacheService.get(key, SsoLoginResult.class);
        if (result == null) {
            throw new SsoAuthenticationException("SSO login code is invalid or expired");
        }
        redisCacheService.delete(key);
        return result;
    }

    private User findLocalUser(SsoUserInfo ssoUserInfo) {
        String localUsername = ssoUserInfo.getLocalUsername();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", localUsername);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new SsoAuthenticationException("用户未开通本系统权限，请联系管理员");
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            throw new SsoAuthenticationException("用户角色未配置，请联系管理员");
        }
        return user;
    }
}
