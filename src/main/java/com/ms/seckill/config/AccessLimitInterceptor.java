package com.ms.seckill.config;

import com.ms.seckill.annotation.AccessLimit;
import com.ms.seckill.pojo.User;
import com.ms.seckill.service.IUserService;
import com.ms.seckill.utils.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * @author MS
 * @create 2022-09-06-10:15
 */
@Component
public class AccessLimitInterceptor implements HandlerInterceptor {
    @Autowired
    private IUserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            User user = getUser(request,response);
            // 将user放入到线程中，参数解析器可以直接从线程中获取
            UserContext.setUser(user);
            HandlerMethod hm = (HandlerMethod) handler;
            AccessLimit annotation = hm.getMethodAnnotation(AccessLimit.class);
            // 没有这个注解，直接放行
            if (annotation == null) {
                return true;
            }
            int maxCount = annotation.maxCount();
            int second = annotation.second();
            boolean needLogin = annotation.needLogin();

            String key = request.getRequestURI();
            if (needLogin) {
                // 需要登录
                if (user == null) {
                    return false;
                }
                key = key + ":" + user.getId();
            }
            ValueOperations valueOperations = redisTemplate.opsForValue();
            Integer count = ((Integer) valueOperations.get(key));
            if (count == null) {
                valueOperations.set(key,1,second, TimeUnit.SECONDS);
            }else if (count < maxCount){
                valueOperations.increment(key);
            }else {
                return false;
            }
        }
        return true;
    }

    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String userTicket = CookieUtil.getCookieValue(request, "userTicket");
        if (null == userTicket) {
            return null;
        }
        User user = userService.getUserByCookie(userTicket, request, response);
        return user;
    }
}
