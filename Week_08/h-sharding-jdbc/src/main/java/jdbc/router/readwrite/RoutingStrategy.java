package jdbc.router.readwrite;


import javax.sql.DataSource;
import java.util.List;

/**
 * 数据路路由策略
 */
public interface RoutingStrategy {
    DataSource routing(List<DataSource> dataSources);
}
