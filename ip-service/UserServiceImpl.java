@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ThirdPartyAPIService thirdPartyAPIService;
    
    @Override
    public void requestDeleteIP(String userId, String ipId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        user.getConnectionConfig().getIpWhitelist().stream()
            .filter(ip -> ip.getId().equals(ipId))
            .findFirst()
            .ifPresent(ip -> {
                ip.setStatus("pending_delete");
                ip.setRequestDeleteTime(new Date());
                userRepository.save(user);
                
                // 记录操作日志
                logOperation(userId, ipId, "request_delete", 
                    ip.getStatus(), "pending_delete");
            });
    }
    
    @Override
    public List<IPWhitelist> findPendingDeleteIPs() {
        return userRepository.findAll()
            .stream()
            .flatMap(user -> user.getConnectionConfig().getIpWhitelist().stream())
            .filter(ip -> "pending_delete".equals(ip.getStatus()))
            .collect(Collectors.toList());
    }

    // UserServiceImpl.java
@Override
public User createUser(UserDTO userDTO) {
    User user = new User();
    user.setUsername(userDTO.getUsername());
    
    // 处理IP列表
    if (userDTO.getIpList() != null) {
        ConnectionConfig config = new ConnectionConfig();
        userDTO.getIpList().forEach(ip -> {
            IPWhitelist ipObj = new IPWhitelist();
            ipObj.setId(UUID.randomUUID().toString());
            ipObj.setIp(ip);
            ipObj.setStatus("pending_activate"); // 初始状态
            ipObj.setCreateTime(new Date());
            config.getIpWhitelist().add(ipObj);
        });
        user.setConnectionConfig(config);
    }
    
    User savedUser = userRepository.save(user);
    
    // 记录操作日志
    logOperation(savedUser.getId(), "user_create", null, null);
    
    return savedUser;
}
// UserServiceImpl.java
@Override
@Transactional
public User updateUser(String userId, UserDTO userDTO) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // 获取当前IP列表
    Set<String> currentIPs = user.getConnectionConfig().getIpWhitelist().stream()
        .map(IPWhitelist::getIp)
        .collect(Collectors.toSet());
    
    // 获取新IP列表
    Set<String> newIPs = new HashSet<>(userDTO.getIpList());
    
    // 计算差异
    Set<String> addedIPs = new HashSet<>(newIPs);
    addedIPs.removeAll(currentIPs);
    
    Set<String> removedIPs = new HashSet<>(currentIPs);
    removedIPs.removeAll(newIPs);
    
    // 处理新增IP
    addedIPs.forEach(ip -> {
        IPWhitelist ipObj = new IPWhitelist();
        ipObj.setId(UUID.randomUUID().toString());
        ipObj.setIp(ip);
        ipObj.setStatus("pending_activate");
        ipObj.setCreateTime(new Date());
        user.getConnectionConfig().getIpWhitelist().add(ipObj);
        
        // 记录新增日志
        logOperation(userId, "ip_add", null, "pending_activate");
    });
    
    // 处理删除IP
    removedIPs.forEach(ip -> {
        user.getConnectionConfig().getIpWhitelist().stream()
            .filter(i -> i.getIp().equals(ip))
            .findFirst()
            .ifPresent(ipObj -> {
                if ("activated".equals(ipObj.getStatus())) {
                    // 已激活IP标记为待删除
                    ipObj.setStatus("pending_delete");
                    ipObj.setRequestDeleteTime(new Date());
                    
                    // 记录删除请求日志
                    logOperation(userId, "ip_request_delete", "activated", "pending_delete");
                } else {
                    // 未激活IP直接移除
                    user.getConnectionConfig().getIpWhitelist().remove(ipObj);
                    
                    // 记录删除日志
                    logOperation(userId, "ip_delete", "pending_activate", "deleted");
                }
            });
    });
    
    return userRepository.save(user);
}
}

