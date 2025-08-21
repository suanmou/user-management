// PendingActionDTO.java
@Data
public class PendingActionDTO {
    private String userId;
    private String username;
    private String ipId;
    private String ipAddress;
    private String actionType; // 'activate' or 'delete'
    private Date requestTime;
    
    // 示例响应结构
    /*
    {
      "userId": "5f8d8f7b8f8f8f8f8f8f8f8f",
      "username": "testuser",
      "ipId": "5f8d8f7b8f8f8f8f8f8f8f8f",
      "ipAddress": "192.168.1.1",
      "actionType": "activate",
      "requestTime": "2023-01-01T00:00:00Z"
    }
    */
}