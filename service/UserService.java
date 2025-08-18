package com.example.user.service;

import com.example.user.entity.IpWhitelist;
import com.example.user.entity.OperationHistory;
import com.example.user.entity.OperationResult;
import com.example.user.entity.User;

import java.util.List;

public interface UserService {
    // 用户管理
    OperationResult createUser(User user, String operator);
    OperationResult createUser(User user, List<IpWhitelist> ipWhitelists, String operator);
    OperationResult updateUser(User user, String operator);
    OperationResult getUserById(String id);
    OperationResult getUserByUsername(String username);
    OperationResult getAllUsers(int page, int size);
    OperationResult deleteUser(String id, String operator);
    
    // IP白名单管理
    OperationResult addIpWhitelist(String userId, IpWhitelist ipWhitelist, String operator);
    OperationResult updateIpWhitelist(String userId, IpWhitelist ipWhitelist, String operator);
    OperationResult deleteIpWhitelist(String userId, String ipId, String operator);
    OperationResult getIpWhitelistsByUserId(String userId);
    
    // 历史记录与回滚
    List<OperationHistory> getOperationHistories(String userId);
    OperationResult rollbackToVersion(String userId, String historyId, String operator);
}
