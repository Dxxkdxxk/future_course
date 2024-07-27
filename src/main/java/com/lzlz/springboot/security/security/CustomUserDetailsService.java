package com.lzlz.springboot.security.security;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lzlz.springboot.security.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService, UserDetailsManager {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 根据用户名查询用户信息
     * @param username
     * @return org.springframework.security.core.userdetails.UserDetails
     * @author liuzheng
     * @create 2024-07-27
     **/
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            return null;
        }
        return user;
    }

    /**
     * 创建新用户
     * @param user
     * @return void
     * @author liuzheng
     * @create 2024-07-27
     **/
    @Override
    public void createUser(UserDetails user) {
        User newUser = (User) user;
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        userMapper.insert(newUser);
    }

    @Override
    public void updateUser(UserDetails user) {
        User updateUser = (User) user;
        userMapper.updateById(updateUser);
    }

    @Override
    public void deleteUser(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        userMapper.delete(queryWrapper);
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        // Implement password change logic if required
    }

    @Override
    public boolean userExists(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectCount(queryWrapper) > 0;
    }
}

