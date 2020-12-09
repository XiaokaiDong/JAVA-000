package jdbc.datasource.readwrite;

import jdbc.router.readwrite.RoutingStrategy;
import jdbc.router.readwrite.RoutingStrategyFactory;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.datasource.AbstractDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor
public class MultiDataSources extends AbstractDataSource {
    private List<DataSource> dataSourceList = new CopyOnWriteArrayList<>();

    boolean readOnly = false;

    /**
     * 将路由策略放在ThreadLocal中
     */
    private static final ThreadLocal<RoutingStrategy> STRATEGY_ID = new ThreadLocal<>();

    static {
        STRATEGY_ID.set(RoutingStrategyFactory.getRouter("TRIVIAL"));
    }

    public static void setRoutingStrategy(String strategy) {
        STRATEGY_ID.set(RoutingStrategyFactory.getRouter(strategy));
    }

    public void init(MultiDataSourceProperties multiDataSourceProperties) {
        readOnly = multiDataSourceProperties.isReadOnly();
        for (DataSourceProperties dataSourceProperties : multiDataSourceProperties.getDataSourcePropertiesList() ) {
            DataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().build();
            dataSourceList.add(dataSource);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return STRATEGY_ID.get().routing(dataSourceList).getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return STRATEGY_ID.get().routing(dataSourceList).getConnection(username, password);
    }


}
