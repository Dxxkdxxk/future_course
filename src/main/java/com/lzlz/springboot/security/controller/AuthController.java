package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.domain.AuthRequest;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.response.ApiResponse;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;



    @PostMapping("/login")
    public ApiResponse<Object> login(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null){ // TODO: 修改返回格式
            return new ApiResponse<>(1, "用户名不存在", null);
        }
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.getPassword()));


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
}
