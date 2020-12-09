package jdbc.sharding.algorithm;

import jdbc.sharding.algorithm.impl.ModHShardingAlgorithm;

import java.util.HashMap;
import java.util.Map;

public class HShardingAlgorithmFactory {

    private static final Map<String, HShardingAlgorithm> shardingAlgorithm = new HashMap<>();

    static {
        //shardingAlgorithm.put("modOnKey", new ModHShardingAlgorithm().setShardingDataSourceProperties());
    }
}
