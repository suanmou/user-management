package com.example.user.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度必须在2-50个字符之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String phone;
    
    private String department;
    
    private boolean enabled = true;
    
    private String role;
    
    private String remark;
    
    private List<IpWhitelist> ipWhitelists = new ArrayList<>();
    
    private Date createTime;
    
    private Date lastLoginTime;
    
    // 自动设置创建时间
    public void setCreateTimeIfAbsent() {
        if (this.createTime == null) {
            this.createTime = new Date();
        }
    }
}
