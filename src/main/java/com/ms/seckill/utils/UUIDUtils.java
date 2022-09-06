package com.ms.seckill.utils;

import java.util.UUID;

/**
 * @author MS
 * @create 2022-08-31-9:52
 */
public class UUIDUtils {
    public static String uuid() { return UUID.randomUUID().toString().replace("-", ""); }
}
