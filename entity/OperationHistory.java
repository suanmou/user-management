package com.example.user.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

@Data
@Document(collection = "operation_histories")
public class OperationHistory {
    @Id
    private String id;
    
    private String userId;
    
    private String ipId; // 可为空，用于标识IP白名单操作
    
    private String operator;
    
    private OperationType operationType;
    
    private String content; // 变更内容JSON
    
    private Date operationTime = new Date();
    
    private boolean success;
    
    private String message;
    
    public enum OperationType {
        USER_ADD, USER_EDIT, USER_DELETE,
        IP_ADD, IP_EDIT, IP_DELETE,
        ROLLBACK
    }
    
    // 构造函数
    public OperationHistory() {
        this.id = UUID.randomUUID().toString();
    }
}
