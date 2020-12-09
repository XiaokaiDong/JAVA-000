package jdbc.datasource.hsharding;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor
@Data
public class HShardingDataSource extends AbstractDataSource {
    private List<DataSource> dataSourceList = new CopyOnWriteArrayList<>();

    private String shardingAlgorithm;
    private String shardingKey;
    private String tableName;

    /**
     * 当前线程专用的数据库索引
     */
    private static final ThreadLocal<Integer> DATASOURCE_IND = new ThreadLocal<>();

    static {
        DATASOURCE_IND.set(0);
    }

    public void init(HShardingDataSourceProperties shardingDataSourceProperties) {
        for (DataSourceProperties dataSourceProperties : shardingDataSourceProperties.getDataSourcePropertiesList() ) {
            DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
            dataSourceList.add(dataSource);
        }
    }

    public static void setDatasourceInd(int index) {
        DATASOURCE_IND.set(index);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSourceList.get(DATASOURCE_IND.get()).getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSourceList.get(DATASOURCE_IND.get()).getConnection(username, password);
    }


}
