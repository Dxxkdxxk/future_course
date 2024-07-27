package com.lzlz.springboot.security.controller;

import com.lzlz.springboot.security.controller.domain.AuthRequest;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import com.lzlz.springboot.security.security.CustomUserDetailsService;
import com.lzlz.springboot.security.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;



    @PostMapping("/login")
    public String login(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        UserDetails user = userDetailsService.loadUserByUsername(username);
        if (user == null){ // TODO: 修改返回格式
            return "Username: " + username + " not found";
        }
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.getPassword()));


        List<String> roles = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        String token = jwtTokenProvider.createToken(username, roles);

        return token; // TODO: 修改返回格式
    }


    @PostMapping("/register")
    public String register(@RequestBody AuthRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (userDetailsService.userExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Creating a new user with default role as ROLE_USER
        User newUser = new User(username, password, List.of("ROLE_USER"));
        userDetailsService.createUser(newUser);

        return "User registered successfully";
    }
}
