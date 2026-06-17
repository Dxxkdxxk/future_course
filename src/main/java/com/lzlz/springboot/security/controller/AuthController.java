package com.lzlz.springboot.security.controller;

import cn.hutool.http.server.HttpServerRequest;
import com.lzlz.springboot.security.config.SsoProperties;
import com.lzlz.springboot.security.domain.AuthRequest;
import com.lzlz.springboot.security.dto.SsoExchangeRequest;
import com.lzlz.springboot.security.dto.SsoLoginResult;
import com.lzlz.springboot.security.exception.SsoAuthenticationException;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.response.ApiResponse;
import com.lzlz.springboot.security.response.ChangePasswordRequest;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.entity.User;
import com.lzlz.springboot.security.service.SsoLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SsoLoginService ssoLoginService;

    @Autowired
    private SsoProperties ssoProperties;

    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) { // 用户名不存在
            return new ApiResponse<>(1, "用户名不存在", null);
        }
    
        // 校验密码是否正确
        if (!passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
            return new ApiResponse<>(1, "用户名或密码错误", null);
        }

        // Cast UserDetails to User to access additional fields
        User user = (User) userDetails;

        // Generate token
        String token = jwtTokenProvider.createToken(username, List.of(user.getRole()));

        // Prepare response data
        var responseData = Map.of(
                "userId", user.getId(),
                "token", token,
                "role", user.getRole()
        );

        return new ApiResponse<>(0, "登录成功", responseData);
    }


    @GetMapping("/sso/login")
    public ApiResponse<Object> ssoLogin() {
        return new ApiResponse<>(0, "success", Map.of("loginUrl", ssoLoginService.buildLoginUrl()));
    }

    @GetMapping("/sso/callback")
    public RedirectView ssoCallback(@RequestParam(value = "ticket", required = false) String ticket) {
        try {
            String code = ssoLoginService.loginByTicketAndCreateCode(ticket);
            String redirectUrl = UriComponentsBuilder.fromUriString(ssoProperties.getFrontendCallbackUrl())
                    .queryParam("code", code)
                    .build()
                    .encode()
                    .toUriString();
            return new RedirectView(redirectUrl);
        } catch (SsoAuthenticationException e) {
            String redirectUrl = UriComponentsBuilder.fromUriString(ssoProperties.getFrontendCallbackUrl())
                    .queryParam("error", e.getMessage())
                    .build()
                    .encode()
                    .toUriString();
            return new RedirectView(redirectUrl);
        }
    }

    @PostMapping("/sso/exchange")
    public ApiResponse<Object> ssoExchange(@RequestBody SsoExchangeRequest request) {
        try {
            SsoLoginResult result = ssoLoginService.exchangeCode(request == null ? null : request.getCode());
            return new ApiResponse<>(0, "success", result);
        } catch (SsoAuthenticationException e) {
            return new ApiResponse<>(1, e.getMessage(), null);
        }
    }

    @GetMapping("/sso/logout")
    public ApiResponse<Object> ssoLogout() {
        return new ApiResponse<>(0, "success", Map.of("logoutUrl", ssoProperties.getLogoutUrl()));
    }

    @PostMapping("/register")
    public ApiResponse<Object> register(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String role = request.getRole();
        String email = request.getEmail();

        if (userDetailsService.userExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Creating a new user with default role as ROLE_USER
        User newUser = new User(username, password, role, email);
        userDetailsService.createUser(newUser);

        // Generate token for the new user
        String token = jwtTokenProvider.createToken(username, List.of(role));

        // Prepare response data
        var responseData = Map.of(
                "userId", newUser.getId(),
                "token", token
        );

        return new ApiResponse<>(0, "注册成功", responseData);
    }

    @PostMapping("/change-password")
    public ApiResponse<Object> changePassword(
            HttpServletRequest httpRequest,
            @RequestBody ChangePasswordRequest request) {

        String token = jwtTokenProvider.resolveToken(httpRequest);
        String username = jwtTokenProvider.getUsername(token);
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        // 1. 参数校验
        if (username == null || username.trim().isEmpty()) {
            return new ApiResponse<>(1, "用户名不能为空", null);
        }
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return new ApiResponse<>(1, "旧密码不能为空", null);
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return new ApiResponse<>(1, "新密码不能为空", null);
        }

        // 2. 查询用户是否存在
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
            return new ApiResponse<>(1, "用户不存在", null);
        }

        if (userDetails == null) {
            return new ApiResponse<>(1, "用户不存在", null);
        }

        // 3. 校验旧密码是否正确
        if (!passwordEncoder.matches(oldPassword, userDetails.getPassword())) {
            return new ApiResponse<>(1, "旧密码错误", null);
        }

        // 4. 修改密码（新密码加密后保存）
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        userDetailsService.changePassword(username, encodedNewPassword);

        return new ApiResponse<>(0, "密码修改成功", null);
    }
}
