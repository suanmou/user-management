package com.example.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationResult {
    private boolean success;
    private String message;
    private Object data;
    
    public static OperationResult success(String message, Object data) {
        return new OperationResult(true, message, data);
    }
    
    public static OperationResult success(String message) {
        return new OperationResult(true, message, null);
    }
    
    public static OperationResult failure(String message) {
        return new OperationResult(false, message, null);
    }
    
    public static OperationResult failure(String message, Object data) {
        return new OperationResult(false, message, data);
    }
}
