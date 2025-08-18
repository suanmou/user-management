package com.example.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ConfigurationClient {

    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${config.service.url:http://config-service}")
    private String configServiceUrl;
    
    /**
     * 应用用户配置
     */
    public boolean applyUserConfig(String userId) {
        try {
            String url = configServiceUrl + "/api/config/apply/user/" + userId;
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("应用用户配置结果 - 用户ID: {}, 成功: {}", userId, success);
            return success;
        } catch (Exception e) {
            log.error("调用用户配置服务失败 - 用户ID: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 应用IP白名单配置
     */
    public boolean applyIpWhitelist(String userId, String ipAddress) {
        try {
            String url = configServiceUrl + "/api/config/apply/ip";
            Map<String, String> params = new HashMap<>();
            params.put("userId", userId);
            params.put("ipAddress", ipAddress);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, params, Map.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("应用IP白名单配置结果 - 用户ID: {}, IP: {}, 成功: {}", userId, ipAddress, success);
            return success;
        } catch (Exception e) {
            log.error("调用IP配置服务失败 - 用户ID: {}, IP: {}", userId, ipAddress, e);
            return false;
        }
    }
    
    /**
     * 移除IP白名单配置
     */
    public boolean removeIpWhitelist(String userId, String ipAddress) {
        try {
            String url = configServiceUrl + "/api/config/remove/ip";
            Map<String, String> params = new HashMap<>();
            params.put("userId", userId);
            params.put("ipAddress", ipAddress);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(url, params, Map.class);
            boolean success = response.getStatusCode().is2xxSuccessful();
            log.info("移除IP白名单配置结果 - 用户ID: {}, IP: {}, 成功: {}", userId, ipAddress, success);
            return success;
        } catch (Exception e) {
            log.error("调用IP移除服务失败 - 用户ID: {}, IP: {}", userId, ipAddress, e);
            return false;
        }
    }
}
