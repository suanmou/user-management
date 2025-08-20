import quickfix.Session;
import quickfix.SessionID;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 市场数据分发器，负责将实时数据推送给所有订阅者
 */
public class MarketDataDistributor {
    private final MarketDataSubscriptionManager subscriptionManager;
    private final MarketDataProvider dataProvider;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    
    public MarketDataDistributor(MarketDataSubscriptionManager manager, MarketDataProvider provider) {
        this.subscriptionManager = manager;
        this.dataProvider = provider;
        startDistributionService();
    }
    
    /**
     * 启动数据分发服务
     */
    private void startDistributionService() {
        // 每秒检查一次数据更新并推送给订阅者
        executor.scheduleAtFixedRate(this::distributeUpdates, 0, 1, TimeUnit.SECONDS);
    }
    
    /**
     * 分发市场数据更新
     */
    private void distributeUpdates() {
        try {
            // 获取最新市场数据更新
            List<MarketData> updates = dataProvider.getLatestUpdates();
            if (updates.isEmpty()) {
                return;
            }
            
            // 遍历所有活跃订阅，推送更新
            for (Subscription subscription : subscriptionManager.getAllActiveSubscriptions()) {
                // 检查是否到了更新时间
                if (!subscription.needsUpdate()) {
                    continue;
                }
                
                SessionID sessionId = subscription.getSessionId();
                Session session = Session.lookupSession(sessionId);
                
                // 检查会话是否活跃
                if (session == null || !session.isLoggedOn()) {
                    subscriptionManager.removeAllSubscriptions(sessionId);
                    continue;
                }
                
                // 生成并发送符合订阅条件的更新
                MarketDataSnapshotFullRefresh updateMessage = createUpdateMessage(subscription, updates);
                if (updateMessage != null) {
                    Session.sendToTarget(updateMessage, sessionId);
                    subscription.setLastUpdateTime(System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            System.err.println("Error distributing market data updates: " + e.getMessage());
        }
    }
    
    /**
     * 创建符合订阅条件的更新消息
     */
    private MarketDataSnapshotFullRefresh createUpdateMessage(Subscription subscription, List<MarketData> updates) {
        MarketDataSnapshotFullRefresh message = new MarketDataSnapshotFullRefresh();
        
        int entryCount = 0;
        for (MarketData data : updates) {
            // 检查是否符合订阅条件
            if (subscription.isSubscribedTo(data.getSymbol()) && 
                subscription.isSubscribedToType(data.getEntryType())) {
                
                // 添加市场数据条目
                MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
                group.set(new MDEntryType(data.getEntryType()));
                group.set(new Symbol(data.getSymbol()));
                group.set(new MDEntryPrice(data.getPrice()));
                group.set(new MDEntrySize(data.getSize()));
                group.set(new MDEntryTime(data.getUpdateTime()));
                
                message.addGroup(group);
                entryCount++;
                
                // 限制单条消息大小，避免过大
                if (entryCount >= 50) {
                    break;
                }
            }
        }
        
        return entryCount > 0 ? message : null;
    }
    
    /**
     * 关闭分发服务
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
    