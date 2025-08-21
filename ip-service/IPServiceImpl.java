@Service
public class IPServiceImpl implements IPService {
    
    @Override
    @Transactional
    public void realDeleteIP(String userId, String ipId) {
        // 1. 调用第三方API删除
        boolean apiSuccess = thirdPartyAPIService.deleteIP(ipId);
        
        if (apiSuccess) {
            // 2. 从MongoDB中移除
            userRepository.pullIPWhitelist(userId, ipId);
            
            // 3. 记录操作日志
            logOperation(userId, ipId, "real_delete", 
                "pending_delete", "deleted");
        }
    }
    @Override
    public List<PendingActionDTO> findPendingActionIPs() {
        // 查询所有需要二次操作的IP
        List<User> users = userRepository.findUsersWithPendingActions();
        
        return users.stream()
            .flatMap(user -> user.getConnectionConfig().getIpWhitelist().stream()
                .filter(ip -> "pending_activate".equals(ip.getStatus()) || 
                              "pending_delete".equals(ip.getStatus()))
                .map(ip -> {
                    PendingActionDTO dto = new PendingActionDTO();
                    dto.setUserId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setIpId(ip.getId());
                    dto.setIpAddress(ip.getIp());
                    dto.setActionType(
                        "pending_activate".equals(ip.getStatus()) ? "activate" : "delete");
                    dto.setRequestTime(
                        "pending_activate".equals(ip.getStatus()) ? 
                            ip.getCreateTime() : ip.getRequestDeleteTime());
                    return dto;
                })
            ).collect(Collectors.toList());
    }
    // IPServiceImpl.java
@Override
@Transactional
public void activateIPs(List<String> ipIds) {
    ipIds.forEach(ipId -> {
        // 查找所有包含该IP的用户
        List<User> users = userRepository.findByIpWhitelistId(ipId);
        
        users.forEach(user -> {
            user.getConnectionConfig().getIpWhitelist().stream()
                .filter(ip -> ip.getId().equals(ipId))
                .findFirst()
                .ifPresent(ip -> {
                    if ("pending_activate".equals(ip.getStatus())) {
                        // 调用第三方接口激活
                        boolean activated = thirdPartyAPIService.activateIP(ip.getIp());
                        
                        if (activated) {
                            ip.setStatus("activated");
                            ip.setActivateTime(new Date());
                            userRepository.save(user);
                            
                            // 记录激活日志
                            logOperation(user.getId(), "ip_activate", "pending_activate", "activated");
                        }
                    }
                });
        });
    });
}
}