package io.kimmking.dubbo.demo.provider.service;

import io.kimmking.entity.Account;
import io.kimmking.mapper.AccountMapper;
import io.kimmking.service.TransferService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.hmily.annotation.HmilyTCC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@DubboService(retries = 0)
public class TransferServiceImpl implements TransferService {

    @Autowired(required = false)
    private AccountMapper accountMapper;

    @Override
    @Transactional
    @HmilyTCC(confirmMethod = "confirm", cancelMethod = "cancel")
    public boolean transferMoney(Account account) {
        log.info("transfering to BGL...");

        boolean from = accountMapper.transferOut(account) > 0;
        boolean to = accountMapper.transferIn2Bgl(account) > 0;

        return from && to;
    }

    /**
     * 因为在try步已经把资金转入了内部账户，所以此时什么也不需要做
     * @param account
     * @return
     */
    public boolean confirm(Account account) {
        return true;
    }

    public boolean cancel(Account account) {
        log.info("begin to inverse...");
        //将金额取负数
        account.setAmount(-account.getAmount());
        boolean from = accountMapper.transferOut(account) > 0;
        boolean to = accountMapper.transferIn2Bgl(account) > 0;

        return from && to;
    }
}
