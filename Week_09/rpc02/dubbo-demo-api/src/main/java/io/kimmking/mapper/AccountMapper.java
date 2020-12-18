package io.kimmking.mapper;

import io.kimmking.entity.Account;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AccountMapper {

//    @Select("select * from account where user_id = #{user_id} and accountType = #{accountType}")
//    Account findByUserId(long user_id, String accountType);

    @Update("update account set amount = amount + #{amount} " +
            "where accountNum=#{accountNum} and currencyType = #{currencyType}")
    int transferOut(Account account);

    @Update("update bgl_account set amount = amount - #{amount} " +
            "where accountNum=#{accountNum} and currencyType = #{currencyType}")
    int transferIn2Bgl(Account account);
}
