package com.ms.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author MS
 * @create 2022-08-30-15:51
 */
@Getter
@ToString
@AllArgsConstructor
public enum RespBeanEnum {
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"服务端异常"),
    LOGIN_ERROR(422,"用户名密码错误"),
    MOBILE_ERROR(423,"手机号码错误"),

    // 秒杀模块
    EMPTY_STOCK(600601,"库存不足"),
    REPEATE_ERROR(600602,"该商品没人限购一个");

    ;

    private final Integer code;
    private final String message;
}
