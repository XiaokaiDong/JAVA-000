package io.kimmking.mapper;

import io.kimmking.entity.Account;
import io.kimmking.entity.ExchangeDeal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ExchangeDealMapper {
    /**
     * 保存交易流水.
     *
     * @param deal 外汇交易
     * @return rows int
     */
    @Insert(" insert into `deal_jrn` (createTime,dealId,status,total_amount,accountNumDebit,accountNumCredit) " +
            " values ( #{createTime},#{dealId},#{status},#{total_amount},#{accountNumDebit},#{accountNumCredit})")
    int save(ExchangeDeal deal);

    /**
     * 更新订单.
     *
     * @param deal 外汇交易
     * @return rows updated
     */
    @Update("update `order` set status = #{status}  where number = #{dealId}")
    int update(ExchangeDeal deal);

}
