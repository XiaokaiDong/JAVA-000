import java.util.HashMap;
import java.util.Map;

public class TransformStrategyFactory {
    private static final Map<String, TransformStrategy> strategies = new HashMap<>();

    static {
        strategies.put("NEG", new NegTransformStrategy());
    }

    public static TransformStrategy getStrategy(String type){
        if (type == null || type.isEmpty()) {      
            throw new IllegalArgumentException("type should not be empty.");    
        }    
        return strategies.get(type);
    }
}
