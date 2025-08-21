// User.java
@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private ConnectionConfig connectionConfig;
    
    @Data
    public static class ConnectionConfig {
        private List<IPWhitelist> ipWhitelist = new ArrayList<>();
    }
}

// IPWhitelist.java
@Data
public class IPWhitelist {
    private String id;
    private String ip;
    private String status; // pending_activate|activated|pending_delete|deleted
    private Date createTime;
    private Date requestDeleteTime;
    private Date deleteTime;
}