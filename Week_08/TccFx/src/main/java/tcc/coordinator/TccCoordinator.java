package tcc.coordinator;

import tcc.service.TccService;

public interface TccCoordinator {
    void registerService(TccService tccService);
    void startTccTx();
    void commit();
    void rollback();
}
