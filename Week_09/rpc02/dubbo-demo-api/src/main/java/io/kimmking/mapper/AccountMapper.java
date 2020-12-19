package io.kimmking.mapper;

import io.kimmking.entity.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

public interface AccountMapper {
    @Insert("insert into `account` (accountNum, amount, currencyType, accountType) " +
            "values ( #{accountNum}, #{amount}, #{currencyType}, #{accountType})")
    int openAccount(Account account);

    @Update("update account set amount = amount + #{amountOut} " +
            "where accountNum=#{accountNum} and currencyType = #{currencyType}")
    int transferOut(Account account,  int amountOut);
}
