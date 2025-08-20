package com.example.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    private String fullName;
    
    @Indexed(unique = true)
    private String email;
    
    private String phone;
    
    private String department;
    
    private String role; // ADMIN, USER, GUEST
    
    private boolean active = true;
    
    private String remark;
    
    private List<IpWhitelist> ipWhitelists;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLoginAt;
}
