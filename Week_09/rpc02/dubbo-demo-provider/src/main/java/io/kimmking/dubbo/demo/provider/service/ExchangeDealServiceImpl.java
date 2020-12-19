package io.kimmking.dubbo.demo.provider.service;

import io.kimmking.entity.Account;
import io.kimmking.entity.DealStatusEnum;
import io.kimmking.entity.ExchangeDeal;
import io.kimmking.mapper.ExchangeDealMapper;
import io.kimmking.rate.RateUtil;
import io.kimmking.service.AccountService;
import io.kimmking.service.ExchangeDealService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.hmily.annotation.HmilyTCC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@DubboService(retries = 0)
public class ExchangeDealServiceImpl implements ExchangeDealService {

    @Autowired
    AccountService accountServiceOne;

    @Autowired
    AccountService accountServiceTwo;

    @Autowired
    ExchangeDealMapper exchangeDealMapper;


    @Override
    @HmilyTCC(confirmMethod = "confirmDealStatus", cancelMethod = "cancelDealStatus")
    public boolean buyUSDWithCNY(Account USDAccount, Account CNYAccount, int amountUSD) {
        ExchangeDeal deal = saveDealBuyUSDWithCNY(USDAccount, CNYAccount,amountUSD);

        //先将人民币账户的钱转入清算用BGL
        int amountCNY = RateUtil.USD2CNY(amountUSD);
        accountServiceOne.transferMoney(CNYAccount, amountCNY);
        updateDealStatus(deal, DealStatusEnum.DEBIT_FINISHED);

        //再将美元账户的头寸转入清算用BGL
        accountServiceTwo.transferMoney(USDAccount, amountUSD);
        updateDealStatus(deal, DealStatusEnum.CREDIT_FINISHED);

        return false;
    }

    private ExchangeDeal saveDealBuyUSDWithCNY(Account USDAccount, Account CNYAccount, int amountUSD){
        final ExchangeDeal deal = buildDealBuyUSDWithCNY(USDAccount, CNYAccount,amountUSD);
        exchangeDealMapper.save(deal);
        return deal;
    }

    private ExchangeDeal buildDealBuyUSDWithCNY(Account USDAccount, Account CNYAccount, int amountUSD){
        ExchangeDeal deal = new ExchangeDeal();
        deal.setCreateTime(new Date());
        deal.setDealId(UUID.randomUUID().toString());
        deal.setAccountNumCredit(USDAccount.getAccountNum());
        deal.setAccountNumDebit(CNYAccount.getAccountNum());
        deal.setTotalAmount(amountUSD);
        deal.setStatus(DealStatusEnum.STARTED.getCode());
        return deal;
    }

    private void updateDealStatus(ExchangeDeal deal, DealStatusEnum dealStatus){
        deal.setStatus(dealStatus.getCode());
        exchangeDealMapper.update(deal);
    }

    public void confirmOrderStatus(ExchangeDeal deal) {
        updateDealStatus(deal, DealStatusEnum.SUCCESS);
        log.info("=========confirm操作完成================");
    }

    public void cancelOrderStatus(ExchangeDeal deal) {
        updateDealStatus(deal, DealStatusEnum.FAILED);
        log.info("=========cancel操作完成================");
    }
}
