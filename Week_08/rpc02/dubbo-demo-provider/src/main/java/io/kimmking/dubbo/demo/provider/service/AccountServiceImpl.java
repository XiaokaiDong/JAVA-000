package io.kimmking.dubbo.demo.provider.service;

import io.kimmking.entity.Account;
import io.kimmking.mapper.AccountMapper;
import io.kimmking.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hmily.annotation.HmilyTCC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class AccountServiceImpl implements AccountService {
    @Autowired(required = false)
    private AccountMapper accountMapper;

    @Override
    @Transactional
    public boolean openAccount(Account account) {
        log.info("Open a account wiht account num = %d ...", account.getAccountNum());
        return accountMapper.openAccount(account) > 0;
    }

    @Autowired()
    private Account bglCleaningForCNY;

    @Autowired
    private Account bglCleaningForUSD;

    /**
     *
     * @param from 转出账户
     * @param amount 金额，用正负表示借贷方向
     * @return
     */
    @Override
    @HmilyTCC(confirmMethod = "confirm", cancelMethod = "cancel")
    @Transactional
    public boolean transferMoney(Account from, int amount) {
        log.info("transfering to BGL...");
        Account targetBgl = from.getCurrencyType().equals("CNY") ? bglCleaningForCNY : bglCleaningForUSD;
        boolean outResult = accountMapper.transferOut(from, amount) > 0;
        boolean inResult = accountMapper.transferOut(targetBgl, -amount) > 0;
        return outResult && inResult;
        //int amountTransfer = RateUtil.CNY2USD(amount);
    }

    /**
     * 因为在try步已经把资金转入了内部账户，所以此时什么也不需要做
     * @param from
     * @return
     */
    public boolean confirm(Account from) {
        return true;
    }

    public boolean cancel(Account from, int amount) {
        log.info("begin to inverse...");
        //将金额取负数
        Account targetBgl = from.getCurrencyType().equals("CNY") ? bglCleaningForCNY : bglCleaningForUSD;
        boolean outRevResult = accountMapper.transferOut(from, -amount) > 0;
        boolean inRevResult = accountMapper.transferOut(targetBgl, amount) > 0;
        return outRevResult && inRevResult;
    }
}
