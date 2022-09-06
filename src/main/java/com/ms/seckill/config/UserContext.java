package com.ms.seckill.config;

import com.ms.seckill.pojo.User;

/**
 * @author MS
 * @create 2022-09-06-10:25
 */
public class UserContext {

    private static ThreadLocal<User> userHolder = new ThreadLocal<>();

    public static void setUser(User user){
        userHolder.set(user);
    }

    public static User getUser(){
        return userHolder.get();
    }
}
