package com.example.user.service.impl;

import com.example.user.entity.IpWhitelist;
import com.example.user.entity.OperationHistory;
import com.example.user.entity.OperationResult;
import com.example.user.entity.User;
import com.example.user.exception.BusinessException;
import com.example.user.repository.OperationHistoryRepository;
import com.example.user.repository.UserRepository;
import com.example.user.service.ConfigurationClient;
import com.example.user.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OperationHistoryRepository historyRepository;
    
    @Autowired
    private ConfigurationClient configurationClient;
    
    @Autowired
    private ObjectMapper objectMapper;

    // 用户管理实现
    
    @Override
    @Transactional
    public OperationResult createUser(User user, String operator) {
        return createUser(user, null, operator);
    }
    
    @Override
    @Transactional
    public OperationResult createUser(User user, List<IpWhitelist> ipWhitelists, String operator) {
        try {
            // 检查用户名和邮箱是否已存在
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
            
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new BusinessException("邮箱已被使用");
            }
            
            // 设置创建时间
            user.setCreateTimeIfAbsent();
            
            // 处理IP白名单（如果有）
            if (ipWhitelists != null && !ipWhitelists.isEmpty()) {
                // 检查IP地址唯一性
                List<String> ipAddresses = ipWhitelists.stream()
                    .map(IpWhitelist::getIpAddress)
                    .collect(Collectors.toList());
                
                // 检查是否有重复的IP地址
                boolean hasDuplicates = ipAddresses.stream()
                    .collect(Collectors.groupingBy(ip -> ip, Collectors.counting()))
                    .values().stream()
                    .anyMatch(count -> count > 1);
                
                if (hasDuplicates) {
                    throw new BusinessException("IP白名单中含有重复的IP地址");
                }
                
                // 准备IP白名单数据
                for (IpWhitelist ipWhitelist : ipWhitelists) {
                    ipWhitelist.prepareForCreate();
                    ipWhitelist.setActive(false); // 初始设为未激活
                }
                
                // 设置到用户对象
                user.setIpWhitelists(ipWhitelists);
            }
            
            // 保存用户
            User savedUser = userRepository.save(user);
            log.info("创建用户成功 - ID: {}", savedUser.getId());
            
            // 批量激活IP白名单配置
            if (ipWhitelists != null && !ipWhitelists.isEmpty()) {
                boolean allSuccess = true;
                for (IpWhitelist ipWhitelist : savedUser.getIpWhitelists()) {
                    boolean configSuccess = configurationClient.applyIpWhitelist(
                        savedUser.getId(), ipWhitelist.getIpAddress());
                    
                    if (configSuccess) {
                        ipWhitelist.setActive(true);
                    } else {
                        allSuccess = false;
                        log.warn("IP白名单配置生效失败 - 用户ID: {}, IP: {}", 
                                savedUser.getId(), ipWhitelist.getIpAddress());
                    }
                }
                
                // 如果有IP配置失败，更新用户记录
                if (!allSuccess) {
                    userRepository.save(savedUser);
                }
                
                // 记录IP白名单操作历史
                for (IpWhitelist ipWhitelist : savedUser.getIpWhitelists()) {
                    if (ipWhitelist.isActive()) {
                        recordIpHistory(savedUser, null, ipWhitelist, 
                            OperationHistory.OperationType.IP_ADD, 
                            operator, true, "用户创建时添加IP白名单成功");
                    } else {
                        recordIpHistory(savedUser, null, ipWhitelist, 
                            OperationHistory.OperationType.IP_ADD, 
                            operator, false, "用户创建时添加IP白名单失败：配置接口调用失败");
                    }
                }
            }
            
            // 记录用户创建操作历史
            recordUserHistory(null, savedUser, OperationHistory.OperationType.USER_ADD, 
                             operator, true, "用户创建成功");
            
            String message = ipWhitelists != null && !ipWhitelists.isEmpty() 
                ? "用户创建成功，IP白名单已添加" 
                : "用户创建成功";
            
            return OperationResult.success(message, savedUser.getId());
            
        } catch (BusinessException e) {
            log.error("创建用户失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("创建用户异常", e);
            return OperationResult.failure("创建用户失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public OperationResult updateUser(User user, String operator) {
        return updateUser(user, null, operator);
    }
    
    @Override
    @Transactional
    public OperationResult updateUser(User user, List<IpWhitelist> newIpWhitelists, String operator) {
        try {
            if (!StringUtils.hasText(user.getId())) {
                throw new BusinessException("用户ID不能为空");
            }
            
            // 获取旧用户信息
            User oldUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 检查用户名是否已被其他用户使用
            if (!oldUser.getUsername().equals(user.getUsername()) && 
                userRepository.existsByUsername(user.getUsername())) {
                throw new BusinessException("用户名已存在");
            }
            
            // 检查邮箱是否已被其他用户使用
            if (!oldUser.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmail(user.getEmail())) {
                throw new BusinessException("邮箱已被使用");
            }
            
            // 保留创建时间
            user.setCreateTime(oldUser.getCreateTime());
            
            // 处理IP白名单变更
            if (newIpWhitelists != null) {
                // 处理IP白名单的增删改
                handleIpWhitelistChanges(user.getId(), oldUser.getIpWhitelists(), newIpWhitelists, operator);
                user.setIpWhitelists(newIpWhitelists);
            } else {
                // 不修改IP白名单，保持原有
                user.setIpWhitelists(oldUser.getIpWhitelists());
            }
            
            // 保存更新
            User updatedUser = userRepository.save(user);
            log.info("更新用户成功 - ID: {}", updatedUser.getId());
            
            // 调用配置接口使更改生效
            boolean configSuccess = configurationClient.applyUserConfig(updatedUser.getId());
            if (!configSuccess) {
                throw new BusinessException("调用配置接口失败，用户信息更新已回滚");
            }
            
            // 记录操作历史
            recordUserHistory(oldUser, updatedUser, OperationHistory.OperationType.USER_EDIT, 
                             operator, true, "用户更新成功");
            
            String message = newIpWhitelists != null 
                ? "用户更新成功，IP白名单已同步更新" 
                : "用户更新成功";
            
            return OperationResult.success(message, updatedUser.getId());
            
        } catch (BusinessException e) {
            log.error("更新用户失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("更新用户异常", e);
            return OperationResult.failure("更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理IP白名单的变更（新增、更新、删除）
     */
    private void handleIpWhitelistChanges(String userId, List<IpWhitelist> oldIpWhitelists, 
                                       List<IpWhitelist> newIpWhitelists, String operator) {
        if (oldIpWhitelists == null) oldIpWhitelists = List.of();
        if (newIpWhitelists == null) newIpWhitelists = List.of();
        
        // 创建映射以便快速查找
        Map<String, IpWhitelist> oldIpMap = oldIpWhitelists.stream()
            .collect(Collectors.toMap(IpWhitelist::getIpAddress, ip -> ip));
        Map<String, IpWhitelist> newIpMap = newIpWhitelists.stream()
            .collect(Collectors.toMap(IpWhitelist::getIpAddress, ip -> ip));
        
        // 处理需要删除的IP（存在于旧的但不在新的中）
        for (IpWhitelist oldIp : oldIpWhitelists) {
            if (!newIpMap.containsKey(oldIp.getIpAddress())) {
                // 删除IP配置
                configurationClient.removeIpWhitelist(userId, oldIp.getIpAddress());
                recordIpHistory(null, oldIp, null, OperationHistory.OperationType.IP_DELETE, 
                               operator, true, "用户更新时删除IP白名单");
            }
        }
        
        // 处理需要添加或更新的IP
        for (IpWhitelist newIp : newIpWhitelists) {
            if (!oldIpMap.containsKey(newIp.getIpAddress())) {
                // 新增IP
                newIp.prepareForCreate();
                newIp.setActive(false);
                
                // 调用配置接口添加
                boolean success = configurationClient.applyIpWhitelist(userId, newIp.getIpAddress());
                newIp.setActive(success);
                
                recordIpHistory(null, null, newIp, OperationHistory.OperationType.IP_ADD, 
                              operator, success, "用户更新时添加IP白名单");
            } else {
                // 更新现有IP（检查是否有变化）
                IpWhitelist oldIp = oldIpMap.get(newIp.getIpAddress());
                if (!oldIp.equals(newIp)) {
                    // 保留创建时间
                    newIp.setId(oldIp.getId());
                    newIp.setCreateTime(oldIp.getCreateTime());
                    newIp.setActive(false);
                    
                    // 先删除旧配置，再添加新配置
                    configurationClient.removeIpWhitelist(userId, oldIp.getIpAddress());
                    boolean success = configurationClient.applyIpWhitelist(userId, newIp.getIpAddress());
                    newIp.setActive(success);
                    
                    recordIpHistory(null, oldIp, newIp, OperationHistory.OperationType.IP_EDIT, 
                                  operator, success, "用户更新时修改IP白名单");
                } else {
                    // IP没有变化，保持原有状态
                    newIp.setId(oldIp.getId());
                    newIp.setCreateTime(oldIp.getCreateTime());
                    newIp.setActive(oldIp.isActive());
                }
            }
        }
    }
    
    @Override
    public OperationResult getUserById(String id) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            return OperationResult.success("查询成功", user);
        } catch (BusinessException e) {
            return OperationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public OperationResult getUserByUsername(String username) {
        try {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            return OperationResult.success("查询成功", user);
        } catch (BusinessException e) {
            return OperationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public OperationResult getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.findAll(pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("users", users.getContent());
        result.put("total", users.getTotalElements());
        result.put("pages", users.getTotalPages());
        
        return OperationResult.success("查询成功", result);
    }
    
    @Override
    @Transactional
    public OperationResult deleteUser(String id, String operator) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 先删除用户
            userRepository.delete(user);
            log.info("删除用户成功 - ID: {}", id);
            
            // 记录操作历史
            recordUserHistory(user, null, OperationHistory.OperationType.USER_DELETE, 
                             operator, true, "用户删除成功");
            
            return OperationResult.success("用户删除成功");
        } catch (BusinessException e) {
            log.error("删除用户失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("删除用户异常", e);
            return OperationResult.failure("删除用户失败: " + e.getMessage());
        }
    }
    
    // IP白名单管理实现
    
    @Override
    @Transactional
    public OperationResult addIpWhitelist(String userId, IpWhitelist ipWhitelist, String operator) {
        try {
            // 获取用户
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 检查IP是否已存在
            boolean ipExists = user.getIpWhitelists().stream()
                .anyMatch(ip -> ip.getIpAddress().equals(ipWhitelist.getIpAddress()));
            
            if (ipExists) {
                throw new BusinessException("该IP地址已在白名单中");
            }
            
            // 准备IP记录
            ipWhitelist.prepareForCreate();
            
            // 添加到用户白名单
            user.getIpWhitelists().add(ipWhitelist);
            User updatedUser = userRepository.save(user);
            log.info("添加IP白名单成功 - 用户ID: {}, IP: {}", userId, ipWhitelist.getIpAddress());
            
            // 调用接口使IP配置生效
            boolean configSuccess = configurationClient.applyIpWhitelist(userId, ipWhitelist.getIpAddress());
            if (configSuccess) {
                // 生效成功，更新IP状态
                ipWhitelist.setActive(true);
                userRepository.save(updatedUser);
            } else {
                throw new BusinessException("IP白名单添加成功，但调用生效接口失败");
            }
            
            // 记录操作历史
            recordIpHistory(user, null, ipWhitelist, OperationHistory.OperationType.IP_ADD, 
                           operator, true, "IP白名单添加成功");
            
            return OperationResult.success("IP白名单添加并生效成功", ipWhitelist.getId());
        } catch (BusinessException e) {
            log.error("添加IP白名单失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("添加IP白名单异常", e);
            recordIpHistory(null, null, ipWhitelist, OperationHistory.OperationType.IP_ADD, 
                           operator, false, e.getMessage());
            return OperationResult.failure("添加IP白名单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public OperationResult updateIpWhitelist(String userId, IpWhitelist ipWhitelist, String operator) {
        try {
            if (!StringUtils.hasText(ipWhitelist.getId())) {
                throw new BusinessException("IP记录ID不能为空");
            }
            
            // 获取用户
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 查找旧IP记录
            IpWhitelist oldIp = user.getIpWhitelists().stream()
                .filter(ip -> ip.getId().equals(ipWhitelist.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("IP记录不存在"));
            
            // 检查IP地址是否已被其他记录使用
            boolean ipExists = user.getIpWhitelists().stream()
                .anyMatch(ip -> !ip.getId().equals(ipWhitelist.getId()) && 
                               ip.getIpAddress().equals(ipWhitelist.getIpAddress()));
            
            if (ipExists) {
                throw new BusinessException("该IP地址已在白名单中");
            }
            
            // 保存旧IP地址用于后续操作
            String oldIpAddress = oldIp.getIpAddress();
            
            // 更新IP记录
            int index = user.getIpWhitelists().indexOf(oldIp);
            user.getIpWhitelists().set(index, ipWhitelist);
            
            // 保留创建时间
            ipWhitelist.setCreateTime(oldIp.getCreateTime());
            ipWhitelist.setActive(false); // 暂时设为未生效
            
            User updatedUser = userRepository.save(user);
            log.info("更新IP白名单成功 - 用户ID: {}, IP: {}", userId, ipWhitelist.getIpAddress());
            
            // 如果IP地址有变化，先移除旧IP
            boolean configSuccess = true;
            if (!oldIpAddress.equals(ipWhitelist.getIpAddress())) {
                configSuccess = configurationClient.removeIpWhitelist(userId, oldIpAddress);
            }
            
            // 应用新IP配置
            if (configSuccess) {
                configSuccess = configurationClient.applyIpWhitelist(userId, ipWhitelist.getIpAddress());
            }
            
            if (configSuccess) {
                // 生效成功，更新IP状态
                ipWhitelist.setActive(true);
                userRepository.save(updatedUser);
            } else {
                throw new BusinessException("IP白名单更新成功，但调用生效接口失败");
            }
            
            // 记录操作历史
            recordIpHistory(user, oldIp, ipWhitelist, OperationHistory.OperationType.IP_EDIT, 
                           operator, true, "IP白名单更新成功");
            
            return OperationResult.success("IP白名单更新并生效成功", ipWhitelist.getId());
        } catch (BusinessException e) {
            log.error("更新IP白名单失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("更新IP白名单异常", e);
            recordIpHistory(null, null, ipWhitelist, OperationHistory.OperationType.IP_EDIT, 
                           operator, false, e.getMessage());
            return OperationResult.failure("更新IP白名单失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public OperationResult deleteIpWhitelist(String userId, String ipId, String operator) {
        try {
            // 获取用户
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 查找IP记录
            IpWhitelist ipToRemove = user.getIpWhitelists().stream()
                .filter(ip -> ip.getId().equals(ipId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("IP记录不存在"));
            
            // 先调用接口移除IP配置
            boolean removeSuccess = configurationClient.removeIpWhitelist(userId, ipToRemove.getIpAddress());
            if (!removeSuccess) {
                throw new BusinessException("调用移除IP接口失败");
            }
            
            // 从用户中删除IP记录
            user.getIpWhitelists().remove(ipToRemove);
            userRepository.save(user);
            log.info("删除IP白名单成功 - 用户ID: {}, IP: {}", userId, ipToRemove.getIpAddress());
            
            // 记录操作历史
            recordIpHistory(user, ipToRemove, null, OperationHistory.OperationType.IP_DELETE, 
                           operator, true, "IP白名单删除成功");
            
            return OperationResult.success("IP白名单删除成功");
        } catch (BusinessException e) {
            log.error("删除IP白名单失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("删除IP白名单异常", e);
            return OperationResult.failure("删除IP白名单失败: " + e.getMessage());
        }
    }
    
    @Override
    public OperationResult getIpWhitelistsByUserId(String userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            return OperationResult.success("查询成功", user.getIpWhitelists());
        } catch (BusinessException e) {
            return OperationResult.failure(e.getMessage());
        }
    }
    
    // 历史记录与回滚实现
    
    @Override
    public List<OperationHistory> getOperationHistories(String userId) {
        return historyRepository.findByUserIdOrderByOperationTimeDesc(userId);
    }
    
    @Override
    @Transactional
    public OperationResult rollbackToVersion(String userId, String historyId, String operator) {
        try {
            // 获取历史记录
            OperationHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new BusinessException("历史记录不存在"));
            
            // 验证历史记录属于指定用户
            if (!history.getUserId().equals(userId)) {
                throw new BusinessException("无权访问此历史记录");
            }
            
            // 获取当前用户信息
            User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            // 保存当前状态作为回滚前记录
            recordUserHistory(currentUser, null, OperationHistory.OperationType.ROLLBACK, 
                             operator, false, "开始回滚操作");
            
            // 执行回滚逻辑（根据历史记录类型处理）
            if (history.getOperationType() == OperationHistory.OperationType.USER_EDIT) {
                // 回滚用户信息
                return rollbackUserEdit(userId, history, currentUser, operator);
            } else if (history.getOperationType().toString().startsWith("IP_")) {
                // 回滚IP白名单
                return rollbackIpChange(userId, history, currentUser, operator);
            }
            
            return OperationResult.success("回滚操作已完成");
        } catch (BusinessException e) {
            log.error("回滚操作失败: {}", e.getMessage());
            return OperationResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("回滚操作异常", e);
            return OperationResult.failure("回滚操作失败: " + e.getMessage());
        }
    }
    
    // 辅助方法
    
    /**
     * 记录用户操作历史
     */
    private void recordUserHistory(User oldUser, User newUser, OperationHistory.OperationType type,
                                  String operator, boolean success, String message) {
        try {
            OperationHistory history = new OperationHistory();
            history.setUserId(newUser != null ? newUser.getId() : (oldUser != null ? oldUser.getId() : null));
            history.setOperator(operator);
            history.setOperationType(type);
            history.setSuccess(success);
            history.setMessage(message);
            
            // 构建变更内容
            Map<String, Object> content = new HashMap<>();
            if (oldUser != null) {
                content.put("oldValue", oldUser);
            }
            if (newUser != null) {
                content.put("newValue", newUser);
            }
            history.setContent(objectMapper.writeValueAsString(content));
            
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("序列化历史记录内容失败", e);
        } catch (Exception e) {
            log.error("记录用户操作历史失败", e);
        }
    }
    
    /**
     * 记录IP白名单操作历史
     */
    private void recordIpHistory(User user, IpWhitelist oldIp, IpWhitelist newIp,
                                OperationHistory.OperationType type, String operator,
                                boolean success, String message) {
        try {
            OperationHistory history = new OperationHistory();
            history.setUserId(user != null ? user.getId() : null);
            history.setIpId(newIp != null ? newIp.getId() : (oldIp != null ? oldIp.getId() : null));
            history.setOperator(operator);
            history.setOperationType(type);
            history.setSuccess(success);
            history.setMessage(message);
            
            // 构建变更内容
            Map<String, Object> content = new HashMap<>();
            if (oldIp != null) {
                content.put("oldValue", oldIp);
            }
            if (newIp != null) {
                content.put("newValue", newIp);
            }
            history.setContent(objectMapper.writeValueAsString(content));
            
            historyRepository.save(history);
        } catch (JsonProcessingException e) {
            log.error("序列化IP历史记录内容失败", e);
        } catch (Exception e) {
            log.error("记录IP操作历史失败", e);
        }
    }
    
    /**
     * 回滚用户编辑操作
     */
    private OperationResult rollbackUserEdit(String userId, OperationHistory history, 
                                            User currentUser, String operator) throws JsonProcessingException {
        // 解析历史记录内容
        Map<String, Object> contentMap = objectMapper.readValue(history.getContent(), Map.class);
        String oldValueJson = objectMapper.writeValueAsString(contentMap.get("oldValue"));
        User oldUser = objectMapper.readValue(oldValueJson, User.class);
        
        // 保留当前的IP白名单和ID
        oldUser.setId(currentUser.getId());
        oldUser.setIpWhitelists(currentUser.getIpWhitelists());
        
        // 保存回滚后的用户信息
        User rolledBackUser = userRepository.save(oldUser);
        
        // 调用配置接口使更改生效
        boolean configSuccess = configurationClient.applyUserConfig(rolledBackUser.getId());
        if (!configSuccess) {
            throw new BusinessException("用户信息回滚成功，但调用配置接口失败");
        }
        
        // 记录回滚历史
        recordUserHistory(currentUser, rolledBackUser, OperationHistory.OperationType.ROLLBACK, 
                         operator, true, "回滚到用户编辑前版本成功");
        
        return OperationResult.success("用户信息回滚成功");
    }
    
    /**
     * 回滚IP白名单变更
     */
    private OperationResult rollbackIpChange(String userId, OperationHistory history,
                                            User currentUser, String operator) throws JsonProcessingException {
        // 解析历史记录内容
        Map<String, Object> contentMap = objectMapper.readValue(history.getContent(), Map.class);
        
        // 根据操作类型执行不同的回滚逻辑
        if (history.getOperationType() == OperationHistory.OperationType.IP_ADD) {
            // 回滚IP添加操作 - 删除对应IP
            String newValueJson = objectMapper.writeValueAsString(contentMap.get("newValue"));
            IpWhitelist addedIp = objectMapper.readValue(newValueJson, IpWhitelist.class);
            
            return deleteIpWhitelist(userId, addedIp.getId(), operator);
        } else if (history.getOperationType() == OperationHistory.OperationType.IP_DELETE) {
            // 回滚IP删除操作 - 重新添加IP
            String oldValueJson = objectMapper.writeValueAsString(contentMap.get("oldValue"));
            IpWhitelist deletedIp = objectMapper.readValue(oldValueJson, IpWhitelist.class);
            
            return addIpWhitelist(userId, deletedIp, operator);
        } else if (history.getOperationType() == OperationHistory.OperationType.IP_EDIT) {
            // 回滚IP编辑操作 - 恢复到编辑前状态
            String oldValueJson = objectMapper.writeValueAsString(contentMap.get("oldValue"));
            IpWhitelist oldIp = objectMapper.readValue(oldValueJson, IpWhitelist.class);
            
            return updateIpWhitelist(userId, oldIp, operator);
        }
        
        return OperationResult.success("IP白名单回滚成功");
    }
}
