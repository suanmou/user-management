import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import java.util.List;

/**
 * 市场数据快照生成器，负责创建和发送市场数据快照
 */
public class MarketDataSnapshotGenerator {

    /**
     * 发送市场数据快照
     */
    public static void sendSnapshot(SessionID sessionId, String reqId, 
                                   MarketDataRequestParams params, MarketDataProvider provider) 
                                   throws SessionNotFound, FieldNotFound {
        
        // 根据请求参数获取相应的市场数据
        List<MarketData> marketDataList = params.isAllSymbols() ? 
            provider.getAllMarketData() : 
            provider.getMarketDataBySymbols(params.getSymbols());
        
        if (marketDataList.isEmpty()) {
            sendEmptySnapshot(sessionId, reqId);
            return;
        }
        
        // 分批发送快照数据，每批最多50条记录
        MarketDataSnapshotFullRefresh snapshot = createNewSnapshot(reqId);
        int count = 0;
        
        for (MarketData data : marketDataList) {
            // 检查是否符合订阅的数据类型
            if (!params.getEntryTypes().contains(data.getEntryType())) {
                continue;
            }
            
            // 添加市场数据条目
            MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
            group.set(new MDEntryType(data.getEntryType()));
            group.set(new Symbol(data.getSymbol()));
            group.set(new MDEntryPrice(data.getPrice()));
            group.set(new MDEntrySize(data.getSize()));
            group.set(new MDEntryTime(data.getUpdateTime()));
            
            snapshot.addGroup(group);
            count++;
            
            // 每50条记录发送一次
            if (count % 50 == 0) {
                Session.sendToTarget(snapshot, sessionId);
                snapshot = createNewSnapshot(reqId);
            }
        }
        
        // 发送剩余记录
        if (count % 50 != 0) {
            Session.sendToTarget(snapshot, sessionId);
        }
    }
    
    /**
     * 创建新的快照消息
     */
    private static MarketDataSnapshotFullRefresh createNewSnapshot(String reqId) {
        MarketDataSnapshotFullRefresh snapshot = new MarketDataSnapshotFullRefresh();
        snapshot.set(new MDReqID(reqId));
        return snapshot;
    }
    
    /**
     * 发送空快照（当没有数据时）
     */
    private static void sendEmptySnapshot(SessionID sessionId, String reqId) throws SessionNotFound {
        MarketDataSnapshotFullRefresh snapshot = new MarketDataSnapshotFullRefresh();
        snapshot.set(new MDReqID(reqId));
        snapshot.set(new Symbol("NO_DATA"));
        Session.sendToTarget(snapshot, sessionId);
    }
}
    