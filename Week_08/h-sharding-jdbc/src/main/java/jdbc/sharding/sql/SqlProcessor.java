package jdbc.sharding.sql;

public interface SqlProcessor {
    Object getShardingKey(String sql, String shardingKey, Object[] args);
    //String getMainTableName(String sql);
    //String getSubTableName(String sql);
}
