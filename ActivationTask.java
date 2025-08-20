package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "activationTasks")
public class ActivationTask {
    @Id
    private String id;
    
    private String userId;
    
    private String userName;
    
    private String ipId;
    
    private String ipAddress;
    
    private ActivationType activationType; // 激活类型：新增、更新、删除
    
    private TaskStatus status; // 任务状态：待处理、处理中、成功、失败
    
    private LocalDateTime createTime;
    
    private LocalDateTime processTime;
    
    private String errorMessage; // 错误信息
}

// 激活类型枚举
enum ActivationType {
    CREATE, UPDATE, DELETE
}

// 任务状态枚举
enum TaskStatus {
    PENDING, PROCESSING, SUCCESS, FAILURE
}
