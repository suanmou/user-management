package com.example.user.controller;

import com.example.user.entity.IpWhitelist;
import com.example.user.entity.OperationResult;
import com.example.user.service.UserService;
import com.example.user.service.UserSessionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users/{userId}/ip-whitelists")
@Api(tags = "IP白名单管理接口")
public class IpWhitelistController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserSessionService sessionService;

    @PostMapping
    @ApiOperation("添加新的IP白名单")
    public OperationResult addIpWhitelist(
            @PathVariable String userId,
            @Valid @RequestBody IpWhitelist ipWhitelist) {
        
        String operator = sessionService.getCurrentUsername();
        return userService.addIpWhitelist(userId, ipWhitelist, operator);
    }
    
    @PutMapping("/{ipId}")
    @ApiOperation("编辑已有IP白名单")
    public OperationResult updateIpWhitelist(
            @PathVariable String userId,
            @PathVariable String ipId,
            @Valid @RequestBody IpWhitelist ipWhitelist) {
        
        ipWhitelist.setId(ipId);
        String operator = sessionService.getCurrentUsername();
        return userService.updateIpWhitelist(userId, ipWhitelist, operator);
    }
    
    @DeleteMapping("/{ipId}")
    @ApiOperation("删除IP白名单")
    public OperationResult deleteIpWhitelist(
            @PathVariable String userId,
            @PathVariable String ipId) {
        
        String operator = sessionService.getCurrentUsername();
        return userService.deleteIpWhitelist(userId, ipId, operator);
    }
    
    @GetMapping
    @ApiOperation("查询用户的所有IP白名单")
    public OperationResult getIpWhitelists(@PathVariable String userId) {
        return userService.getIpWhitelistsByUserId(userId);
    }
}
