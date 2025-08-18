package com.example.user.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.UUID;

@Data
public class IpWhitelist {
    private String id;
    
    @NotBlank(message = "IP地址不能为空")
    @Pattern(regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$", 
             message = "IP地址格式不正确")
    private String ipAddress;
    
    private String description;
    
    private boolean active = false; // 是否已生效
    
    private Date createTime;
    
    // 自动生成ID和创建时间
    public void prepareForCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
