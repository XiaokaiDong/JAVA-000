package jdbc.sharding.algorithm.impl;

import jdbc.datasource.hsharding.HShardingDataSourceProperties;
import jdbc.sharding.algorithm.HShardingAlgorithm;
import lombok.Data;

@Data
public class ModHShardingAlgorithm implements HShardingAlgorithm {

    private HShardingDataSourceProperties shardingDataSourceProperties;

    @Override
    public int getDatabaseIndex(Object key) {
        return (Integer)key % shardingDataSourceProperties.getNumOfDatabases();
    }

    @Override
    public int getTableIndex(Object key) {
        return (Integer)key % shardingDataSourceProperties.getNumOfTablesPerDatabase();
    }
}
