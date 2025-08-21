// IPController.java
@RestController
@RequestMapping("/api/ips")
public class IPController {
    
    @Autowired
    private IPService ipService;
    
    /**
     * 获取需要二次操作的IP列表
     * @return 包含待激活和待删除的IP列表
     */
    @GetMapping("/pending-actions")
    public ResponseEntity<List<PendingActionDTO>> getPendingActionIPs() {
        List<PendingActionDTO> pendingActions = ipService.findPendingActionIPs();
        return ResponseEntity.ok(pendingActions);
    }
}