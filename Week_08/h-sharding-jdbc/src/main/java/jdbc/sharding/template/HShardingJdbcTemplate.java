package jdbc.sharding.template;

import jdbc.datasource.hsharding.HShardingDataSource;
import jdbc.sharding.algorithm.impl.ModHShardingAlgorithm;
import jdbc.sharding.sql.SqlProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

import java.util.List;

@Slf4j
public class HShardingJdbcTemplate extends JdbcTemplate {
    private JdbcTemplate jdbcTemplate;

    private ModHShardingAlgorithm modHShardingAlgorithm;

    private SqlProcessor sqlProcessor;

    private String sql;

    private static final ThreadLocal<Object> shardingKeyHolder = new ThreadLocal<>();

    public HShardingJdbcTemplate(JdbcTemplate jdbcTemplate,
                                 ModHShardingAlgorithm modHShardingAlgorithm,
                                 SqlProcessor sqlProcessor) {
        this.jdbcTemplate = jdbcTemplate;
        this.modHShardingAlgorithm = modHShardingAlgorithm;
        this.sqlProcessor = sqlProcessor;
    }

    public HShardingJdbcTemplate withSql(String sql) {
        this.sql = sql;
        return this;
    }

    //定位某一个库
    private HShardingJdbcTemplate withShardingKey(Object shardingKey) {
        HShardingDataSource shardingDataSource = (HShardingDataSource)jdbcTemplate.getDataSource();
        if (shardingKey != null) {
            shardingKeyHolder.set(shardingKey);
            HShardingDataSource.setDatasourceInd(modHShardingAlgorithm.getDatabaseIndex(shardingKey));
        } else {
            shardingKeyHolder.set(null);
        }
        return this;
    }

    //定位某一张表，不使用SQL解析的简化替代方案
    private HShardingJdbcTemplate withTableName(String tableName) {
        int tableIndex = 0;
        if (shardingKeyHolder.get() != null) {
            tableIndex = modHShardingAlgorithm.getTableIndex(shardingKeyHolder.get());
            String targetTableName = String.format("_%02d", tableIndex);
            sql.replace(tableName, targetTableName);
        }
        return this;
    }

    private void preProcess(Object... args){
        String shardingKey = ((HShardingDataSource)jdbcTemplate.getDataSource()).getShardingKey();
        String tableName = ((HShardingDataSource)jdbcTemplate.getDataSource()).getTableName();
        withShardingKey(sqlProcessor.getShardingKey(sql,shardingKey,args))
                .withTableName(tableName);
    }

    public int update(@Nullable Object... args) {

        preProcess(args);

        if (shardingKeyHolder.get() == null){
            //广播SQL
            return 0;
        }else {
            log.info("the actual sql is " + sql );
            return jdbcTemplate.update(sql, args);
        }
    }

    public int[] batchUpdate(final BatchPreparedStatementSetter pss) throws DataAccessException {
        return jdbcTemplate.batchUpdate(sql, pss);
    }

    public  <T> T queryForObject(@Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        preProcess(args);

        if (shardingKeyHolder.get() == null){
            //广播SQL
            return null;
        }else {
            log.info("the actual sql is " + sql );
            return jdbcTemplate.queryForObject(sql, args, rowMapper);
        }
    }

    public   <T> List<T> query(@Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        preProcess(args);

        if (shardingKeyHolder.get() == null){
            //广播SQL
            return null;
        }else {
            log.info("the actual sql is " + sql );
            return jdbcTemplate.query(sql, args, rowMapper);
        }
    }


}
