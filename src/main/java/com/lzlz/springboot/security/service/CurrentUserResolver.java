package com.lzlz.springboot.security.service;

import com.lzlz.springboot.security.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserResolver {

    public User requireUser(User user) {
        if (user != null) {
            return user;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User currentUser) {
            return currentUser;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
}
