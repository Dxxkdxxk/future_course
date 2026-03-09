package com.lzlz.springboot.security.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
@Data
@AllArgsConstructor
@TableName("users")
public class User implements UserDetails {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

    @NotEmpty
    private String role;

    @NotEmpty
    private String email;
    public User() {
    }

    public User(@NotEmpty String username, @NotEmpty String password, String role, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
    }

    /**
     * 授予不同用户访问权限
     * @return java.util.Collection<? extends org.springframework.security.core.GrantedAuthority>
     * @author liuzheng
     * @create 2024-07-27
     * role:ROLE_开头
     **/
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        System.out.println(role+System.currentTimeMillis());
        return List.of(role).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}