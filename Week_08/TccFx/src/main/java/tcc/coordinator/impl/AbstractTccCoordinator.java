package tcc.coordinator.impl;

import tcc.coordinator.TccCoordinator;
import tcc.service.TccService;

import java.util.concurrent.CopyOnWriteArrayList;

public class AbstractTccCoordinator implements TccCoordinator {

    private CopyOnWriteArrayList<TccService> participator = new CopyOnWriteArrayList<>();

    public void registerService(TccService tccService) {
        participator.addIfAbsent(tccService);
    }

    public void startTccTx() {

    }

    public void commit() {

    }

    public void rollback() {

    }
}
