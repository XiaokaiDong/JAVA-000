package jdbc.router.impl;

import jdbc.router.RoutingStrategy;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRoutingStrategy implements RoutingStrategy {
    private AtomicInteger curIndex = new AtomicInteger();

    @Override
    public DataSource routing(List<DataSource> dataSources) {
        return dataSources.get(curIndex.getAndIncrement() % dataSources.size());
    }
}
