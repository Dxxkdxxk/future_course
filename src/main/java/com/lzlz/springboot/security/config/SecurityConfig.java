package com.lzlz.springboot.security.config;

import com.lzlz.springboot.security.jwt.JwtAuthenticationEntryPoint;
import com.lzlz.springboot.security.jwt.JwtTokenAuthenticationFilter;
import com.lzlz.springboot.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Bean
    public AuthenticationManager authenticationManagerBean(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 对密码进行编码
     * @return org.springframework.security.crypto.password.PasswordEncoder
     * @author liuzheng
     * @create 2024-07-27
     **/
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * 过滤器链
 * @param http
     * @return org.springframework.security.web.SecurityFilterChain
     * @author liuzheng
     * @create 2024-07-27
     **/
    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtTokenAuthenticationFilter customFilter = new JwtTokenAuthenticationFilter(jwtTokenProvider);

        http
                .httpBasic(httpBasic -> httpBasic.disable()) // 禁用HTTP基本认证
                .csrf(csrf -> csrf.disable()) // 禁用csrf保护
                .sessionManagement(sessionManagement -> sessionManagement // 配置会话管理策略，所有请求不使用会话状态
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll() // 登录页和注册页允许所有用户访问
                        .requestMatchers(HttpMethod.GET, "/admin").hasRole("ADMIN") // 定义一个只允许管理员访问的页面
                        .requestMatchers(HttpMethod.GET, "/user").hasRole("USER") // 只允许用户访问的页面
                        .requestMatchers("/api/v1/teacher/course/**").hasAuthority("teacher")
                        .requestMatchers("/api/v1/student/course/**").hasAuthority("student")
                        .requestMatchers("/api/v1/teacher/**").permitAll() // 暂时允许所有人访问
                        .requestMatchers("/api/v1/student/**").permitAll()
                        .requestMatchers("/api/v1/course/**").permitAll()
                        // 允许教师访问学生管理接口
                        .requestMatchers("/api/v1/course/**").permitAll() // 教师
                        .requestMatchers("/api/v1/chapters/**").permitAll() // 教师
                        //.requestMatchers("/api/v1/course/*/question/**").authenticated() // 教师
                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                )
                .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class); // 确定自定义过滤器的位置

        return http.build();
    }
}

