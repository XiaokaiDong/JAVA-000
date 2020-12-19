package io.kimmking.service;

import io.kimmking.entity.Account;
import org.dromara.hmily.annotation.Hmily;

public interface AccountService {
    boolean openAccount(Account account);

    @Hmily
    boolean transferMoney(Account from, int amount);
}
