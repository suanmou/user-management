@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    // 申请删除IP
    @PutMapping("/{userId}/ips/{ipId}/request-delete")
    public ResponseEntity<?> requestDeleteIP(
            @PathVariable String userId,
            @PathVariable String ipId) {
        userService.requestDeleteIP(userId, ipId);
        return ResponseEntity.ok().build();
    }
    
    // 获取待删除IP列表
    @GetMapping("/ips/pending-delete")
    public List<IPWhitelist> getPendingDeleteIPs() {
        return userService.findPendingDeleteIPs();
    }
}

// 独立IP操作控制器
@RestController
@RequestMapping("/api/ips")
public class IPController {
    
    @Autowired
    private IPService ipService;
    
    // 实际删除IP
    @DeleteMapping("/{ipId}")
    public ResponseEntity<?> realDeleteIP(
            @PathVariable String ipId,
            @RequestParam String userId) {
        ipService.realDeleteIP(userId, ipId);
        return ResponseEntity.ok().build();
    }
}