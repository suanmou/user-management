package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(collection = "operationLogs")
public class OperationLog {
    @Id
    private String id;
    
    private String userId; // 关联的用户ID
    
    private String userName; // 关联的用户名
    
    private String ipId; // 关联的IP白名单ID
    
    private OperationType operationType; // 操作类型
    
    private Map<String, Object> oldValue; // 旧值
    
    private Map<String, Object> newValue; // 新值
    
    private String operator; // 操作人
    
    private LocalDateTime operationTime;
    
    private String remarks; // 备注
}

// 操作类型枚举
enum OperationType {
    CREATE_USER,
    UPDATE_USER,
    DELETE_USER,
    ADD_IP,
    UPDATE_IP,
    DELETE_IP,
    ACTIVATE_IP,
    ROLLBACK_IP
}
