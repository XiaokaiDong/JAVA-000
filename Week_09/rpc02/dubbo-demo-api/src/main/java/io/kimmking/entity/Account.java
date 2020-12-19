package io.kimmking.entity;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Account {
    private int accountNum;

    //以分为单位
    private int amount;

    /**
     * 币种
     * USD 美元
     * CNY 人民币
     */
    private String currencyType;

    /**
     * 账户类型
     * BGL 内部账户
     * 000 普通账户
     */
    private String accountType;

}
