package io.kimmking.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DealStatusEnum {
    /**
     * Not pay order status enum.
     */
    STARTED(1, "已开始"),

    /**
     * Paying order status enum.
     */
    DEBIT_FINISHED(2, "借方交易已完成"),

    /**
     * Pay fail order status enum.
     */
    CREDIT_FINISHED(3, "贷方交易已完成"),

    /**
     * Pay success order status enum.
     */
    SUCCESS(4, "交易成功"),

    /**
     * Pay success order status enum.
     */
    FAILED(5, "交易失败");

    private final int code;

    private final String desc;
}
