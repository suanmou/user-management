package com.example.user.controller;

import com.example.user.entity.OperationResult;
import com.example.user.entity.User;
import com.example.user.service.UserService;
import com.example.user.service.UserSessionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Api(tags = "用户管理接口")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserSessionService sessionService;

    @PostMapping
    @ApiOperation("创建新用户")
    public OperationResult createUser(@Valid @RequestBody User user) {
        String operator = sessionService.getCurrentUsername();
        return userService.createUser(user, operator);
    }
    
    @PutMapping("/{id}")
    @ApiOperation("更新用户信息")
    public OperationResult updateUser(@PathVariable String id, @Valid @RequestBody User user) {
        user.setId(id);
        String operator = sessionService.getCurrentUsername();
        return userService.updateUser(user, operator);
    }
    
    @GetMapping("/{id}")
    @ApiOperation("根据ID查询用户")
    public OperationResult getUserById(@PathVariable String id) {
        return userService.getUserById(id);
    }
    
    @GetMapping("/username/{username}")
    @ApiOperation("根据用户名查询用户")
    public OperationResult getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }
    
    @GetMapping
    @ApiOperation("分页查询所有用户")
    public OperationResult getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.getAllUsers(page, size);
    }
    
    @DeleteMapping("/{id}")
    @ApiOperation("删除用户")
    public OperationResult deleteUser(@PathVariable String id) {
        String operator = sessionService.getCurrentUsername();
        return userService.deleteUser(id, operator);
    }
    
    @GetMapping("/{userId}/histories")
    @ApiOperation("查询用户操作历史")
    public OperationResult getUserHistories(@PathVariable String userId) {
        return OperationResult.success("查询成功", userService.getOperationHistories(userId));
    }
    
    @PostMapping("/{userId}/rollback/{historyId}")
    @ApiOperation("回滚到指定历史版本")
    public OperationResult rollbackToVersion(
            @PathVariable String userId,
            @PathVariable String historyId) {
        String operator = sessionService.getCurrentUsername();
        return userService.rollbackToVersion(userId, historyId, operator);
    }
}
