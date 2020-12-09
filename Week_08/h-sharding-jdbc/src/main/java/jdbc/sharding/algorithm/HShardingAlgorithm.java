package jdbc.sharding.algorithm;

public interface HShardingAlgorithm {
    int getDatabaseIndex(Object key);
    int getTableIndex(Object key);
}
