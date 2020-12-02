package jdbc.router.impl;

import jdbc.router.RoutingStrategy;

import javax.sql.DataSource;
import java.util.List;

public class TrivialRoutingStrategy implements RoutingStrategy {
    @Override
    public DataSource routing(List<DataSource> dataSources) {
        return dataSources.get(0);
    }
}
