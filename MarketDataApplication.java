import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataRequestReject;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIX应用层实现，处理市场数据订阅和取消订阅请求
 */
public class MarketDataApplication implements Application {
    private final MarketDataSubscriptionManager subscriptionManager;
    private final MarketDataProvider dataProvider;
    private final MarketDataDistributor distributor;

    public MarketDataApplication() {
        this.subscriptionManager = new MarketDataSubscriptionManager();
        this.dataProvider = new MarketDataProvider();
        this.distributor = new MarketDataDistributor(subscriptionManager, dataProvider);
    }

    @Override
    public void onCreate(SessionID sessionId) {}

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.println("Client logged on: " + sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.println("Client logged out: " + sessionId);
        // 自动取消该会话的所有订阅
        subscriptionManager.removeAllSubscriptions(sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {}

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {}

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {}

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        if (message instanceof MarketDataRequest) {
            handleMarketDataRequest((MarketDataRequest) message, sessionId);
        }
    }

    /**
     * 处理市场数据请求（订阅/取消订阅）
     */
    private void handleMarketDataRequest(MarketDataRequest request, SessionID sessionId) throws FieldNotFound {
        String reqId = request.getMDReqID().getValue();
        char requestType = request.getSubscriptionRequestType().getValue();

        switch (requestType) {
            case SubscriptionRequestType.SNAPSHOT:
                handleSnapshotRequest(request, sessionId, reqId);
                break;
            case SubscriptionRequestType.SNAPSHOT_PLUS_UPDATES:
                handleSubscribeRequest(request, sessionId, reqId);
                break;
            case SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_PLUS_UPDATE:
                handleUnsubscribeRequest(sessionId, reqId);
                break;
            default:
                sendReject(sessionId, reqId, "Unsupported request type");
        }
    }

    /**
     * 处理快照请求（一次性获取数据）
     */
    private void handleSnapshotRequest(MarketDataRequest request, SessionID sessionId, String reqId) throws FieldNotFound {
        try {
            // 解析请求的证券列表和数据类型
            MarketDataRequestParams params = parseRequestParams(request);
            // 生成并发送快照数据
            MarketDataSnapshotGenerator.sendSnapshot(sessionId, reqId, params, dataProvider);
        } catch (Exception e) {
            sendReject(sessionId, reqId, "Failed to generate snapshot: " + e.getMessage());
        }
    }

    /**
     * 处理订阅请求（快照+持续更新）
     */
    private void handleSubscribeRequest(MarketDataRequest request, SessionID sessionId, String reqId) throws FieldNotFound {
        try {
            MarketDataRequestParams params = parseRequestParams(request);
            
            // 创建新订阅
            Subscription subscription = new Subscription(
                sessionId,
                params.getSymbols(),
                params.getEntryTypes(),
                params.getUpdateFrequency(),
                System.currentTimeMillis()
            );
            
            // 保存订阅
            subscriptionManager.addSubscription(reqId, subscription);
            
            // 立即发送初始快照
            MarketDataSnapshotGenerator.sendSnapshot(sessionId, reqId, params, dataProvider);
            
            System.out.println("Created subscription: " + reqId + " for session: " + sessionId);
        } catch (Exception e) {
            sendReject(sessionId, reqId, "Failed to create subscription: " + e.getMessage());
        }
    }

    /**
     * 处理取消订阅请求
     */
    private void handleUnsubscribeRequest(SessionID sessionId, String reqId) {
        try {
            // 验证订阅是否存在且属于当前会话
            if (subscriptionManager.validateSubscriptionOwnership(reqId, sessionId)) {
                // 移除订阅
                subscriptionManager.removeSubscription(reqId);
                System.out.println("Cancelled subscription: " + reqId + " for session: " + sessionId);
                
                // 发送确认（可选，FIX协议没有强制要求）
                sendUnsubscribeConfirmation(sessionId, reqId);
            } else {
                sendReject(sessionId, reqId, "Subscription not found or not owned by this session");
            }
        } catch (Exception e) {
            sendReject(sessionId, reqId, "Failed to cancel subscription: " + e.getMessage());
        }
    }

    /**
     * 解析请求参数
     */
    private MarketDataRequestParams parseRequestParams(MarketDataRequest request) throws FieldNotFound {
        // 解析证券列表（0表示全市场）
        int noRelatedSym = request.getNoRelatedSym().getValue();
        
        // 解析数据类型
        int noMDEntryTypes = request.getNoMDEntryTypes().getValue();
        
        // 解析更新频率（如果有）
        int updateFreq = 1; // 默认1秒
        if (request.isSetMarketDepth()) {
            updateFreq = request.getMarketDepth().getValue();
        }
        
        return new MarketDataRequestParams(noRelatedSym, noMDEntryTypes, updateFreq);
    }

    /**
     * 发送取消订阅确认
     */
    private void sendUnsubscribeConfirmation(SessionID sessionId, String reqId) throws SessionNotFound {
        MarketDataSnapshotFullRefresh confirm = new MarketDataSnapshotFullRefresh();
        confirm.set(new MDReqID(reqId));
        confirm.set(new Symbol("UNSUBSCRIBED"));
        Session.sendToTarget(confirm, sessionId);
    }

    /**
     * 发送请求拒绝消息
     */
    private void sendReject(SessionID sessionId, String reqId, String reason) {
        try {
            MarketDataRequestReject reject = new MarketDataRequestReject();
            reject.set(new MDReqID(reqId));
            reject.set(new MDReqRejReason(MDReqRejReason.UNKNOWN_SYMBOL));
            reject.set(new Text(reason));
            Session.sendToTarget(reject, sessionId);
        } catch (Exception e) {
            System.err.println("Failed to send reject: " + e.getMessage());
        }
    }
}
    