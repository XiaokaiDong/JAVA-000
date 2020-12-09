package jdbc.router.readwrite;

import jdbc.router.readwrite.impl.RoundRobinRoutingStrategy;
import jdbc.router.readwrite.impl.TrivialRoutingStrategy;

import java.util.HashMap;
import java.util.Map;

public class RoutingStrategyFactory {
    private static final Map<String, RoutingStrategy> strategyMap = new HashMap<>();

    static {
        strategyMap.put("TRIVIAL", new TrivialRoutingStrategy());
        strategyMap.put("RR", new RoundRobinRoutingStrategy());
    }

    public static RoutingStrategy getRouter(String type) {
        if (type.equals("TRIVIAL") || type.equals("RR")){
            return strategyMap.get(type);
        } else {
            return strategyMap.get("TRIVIAL");
        }
    }
}
