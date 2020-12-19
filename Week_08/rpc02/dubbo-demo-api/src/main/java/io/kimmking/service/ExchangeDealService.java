package io.kimmking.service;

import io.kimmking.entity.Account;
import org.dromara.hmily.annotation.Hmily;

public interface ExchangeDealService {

    @Hmily
    boolean buyUSDWithCNY(Account USDAccount, Account CNYAccount, int amountUSD);
}
