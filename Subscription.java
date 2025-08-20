import quickfix.SessionID;
import java.util.List;

/**
 * 订阅实体类，保存订阅的详细信息
 */
public class Subscription {
    private final SessionID sessionId;          // 客户端会话ID
    private final List<String> symbols;         // 订阅的证券列表，空表示全市场
    private final List<Character> entryTypes;   // 订阅的数据类型(BID/ASK等)
    private final int updateFrequency;          // 更新频率(秒)
    private final long subscribeTime;           // 订阅时间戳
    private long lastUpdateTime;                // 最后一次推送时间

    public Subscription(SessionID sessionId, List<String> symbols, 
                       List<Character> entryTypes, int updateFrequency, long subscribeTime) {
        this.sessionId = sessionId;
        this.symbols = symbols;
        this.entryTypes = entryTypes;
        this.updateFrequency = updateFrequency;
        this.subscribeTime = subscribeTime;
        this.lastUpdateTime = subscribeTime;
    }

    // Getter和Setter方法
    public SessionID getSessionId() {
        return sessionId;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public List<Character> getEntryTypes() {
        return entryTypes;
    }

    public int getUpdateFrequency() {
        return updateFrequency;
    }

    public long getSubscribeTime() {
        return subscribeTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
    
    /**
     * 检查是否需要更新（根据更新频率）
     */
    public boolean needsUpdate() {
        return System.currentTimeMillis() - lastUpdateTime >= updateFrequency * 1000L;
    }
    
    /**
     * 检查是否订阅了指定证券
     */
    public boolean isSubscribedTo(String symbol) {
        // 空列表表示订阅全市场
        return symbols.isEmpty() || symbols.contains(symbol);
    }
    
    /**
     * 检查是否订阅了指定数据类型
     */
    public boolean isSubscribedToType(char type) {
        return entryTypes.contains(type);
    }
}
    