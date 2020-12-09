package jdbc.router.readwrite.impl;

import jdbc.router.readwrite.RoutingStrategy;

import javax.sql.DataSource;
import java.util.List;

public class TrivialRoutingStrategy implements RoutingStrategy {
    @Override
    public DataSource routing(List<DataSource> dataSources) {
        return dataSources.get(0);
    }
}
