import quickfix.SessionID;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 订阅管理器，负责管理所有市场数据订阅
 */
public class MarketDataSubscriptionManager {
    // 订阅ID到订阅对象的映射
    private final Map<String, Subscription> subscriptionsById = new ConcurrentHashMap<>();
    // 会话ID到订阅ID列表的映射，用于快速查找会话的所有订阅
    private final Map<SessionID, List<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    /**
     * 添加新订阅
     */
    public void addSubscription(String reqId, Subscription subscription) {
        subscriptionsById.put(reqId, subscription);
        
        // 更新会话订阅映射
        sessionSubscriptions.compute(subscription.getSessionId(), (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(reqId);
            return list;
        });
    }

    /**
     * 移除订阅
     */
    public void removeSubscription(String reqId) {
        Subscription subscription = subscriptionsById.remove(reqId);
        if (subscription != null) {
            // 更新会话订阅映射
            SessionID sessionId = subscription.getSessionId();
            sessionSubscriptions.computeIfPresent(sessionId, (key, list) -> {
                list.remove(reqId);
                return list.isEmpty() ? null : list;
            });
        }
    }

    /**
     * 移除指定会话的所有订阅
     */
    public void removeAllSubscriptions(SessionID sessionId) {
        List<String> reqIds = sessionSubscriptions.remove(sessionId);
        if (reqIds != null) {
            for (String reqId : reqIds) {
                subscriptionsById.remove(reqId);
            }
        }
    }

    /**
     * 获取指定订阅ID的订阅
     */
    public Subscription getSubscription(String reqId) {
        return subscriptionsById.get(reqId);
    }

    /**
     * 获取指定会话的所有订阅
     */
    public List<Subscription> getSubscriptionsForSession(SessionID sessionId) {
        List<String> reqIds = sessionSubscriptions.get(sessionId);
        if (reqIds == null || reqIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        return reqIds.stream()
                .map(subscriptionsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 验证订阅是否属于指定会话
     */
    public boolean validateSubscriptionOwnership(String reqId, SessionID sessionId) {
        Subscription subscription = subscriptionsById.get(reqId);
        return subscription != null && subscription.getSessionId().equals(sessionId);
    }

    /**
     * 获取所有活跃订阅
     */
    public Collection<Subscription> getAllActiveSubscriptions() {
        return subscriptionsById.values();
    }
}
    