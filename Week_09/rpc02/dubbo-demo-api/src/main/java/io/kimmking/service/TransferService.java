package io.kimmking.service;

import io.kimmking.entity.Account;
import org.dromara.hmily.annotation.Hmily;

public interface TransferService {

    @Hmily
    boolean transferMoney(Account account);
}
