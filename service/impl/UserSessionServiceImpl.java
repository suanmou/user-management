package com.example.user.service.impl;

import com.example.user.exception.BusinessException;
import com.example.user.service.UserSessionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserSessionServiceImpl implements UserSessionService {

    @Override
    public String getCurrentUsername() {
        // 从安全上下文获取当前登录用户
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        
        if (principal instanceof String) {
            return (String) principal;
        }
        
        throw new BusinessException("无法获取当前登录用户信息");
    }
}
