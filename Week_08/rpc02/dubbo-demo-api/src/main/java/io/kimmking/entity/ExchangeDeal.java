package io.kimmking.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ExchangeDeal {
    private Integer id;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 交易编号
     */
    private String dealId;

    /**
     * 交易状态
     */
    private Integer status;

    /**
     * 金额
     */
    private int totalAmount;


    /**
     * 借方账号
     */
    private int accountNumDebit;

    /**
     * 贷方账号
     */
    private int accountNumCredit;
}
