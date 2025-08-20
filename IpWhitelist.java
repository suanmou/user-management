package com.example.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpWhitelist {
    private String id; // 唯一标识
    
    private String ipAddress; // IP地址
    
    private String subnetMask; // 子网掩码
    
    private String description; // 描述
    
    private IpStatus status; // 状态：PENDING, ACTIVE, INACTIVE
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastActivatedAt;
    
    private String operator; // 操作人
}

// 状态枚举
enum IpStatus {
    PENDING,   // 待激活
    ACTIVE,    // 已激活
    INACTIVE   // 已失效
}
