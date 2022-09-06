package com.ms.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class SeckillDemoApplicationTests {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisScript redisScript;

    @Test
    public void contextLoads() {
        Boolean result = redisTemplate.opsForValue().setIfAbsent("k1", "v1");
        // 成功，说明获取到锁
        if (result) {
            // 进行相应操作
            redisTemplate.opsForValue().set("name","xxxxx");
            String name = (String) redisTemplate.opsForValue().get("name");
            System.out.println(name);
            // 操作完成删除锁
            redisTemplate.delete("name");
        }else {
            System.out.println("有线程在使用，请稍后");
        }
    }


    @Test
    public void testLock(){
        ValueOperations operations = redisTemplate.opsForValue();
        String value = UUID.randomUUID().toString();
        Boolean flag = operations.setIfAbsent("k1", value, 120, TimeUnit.SECONDS);
        if (flag) {
            operations.set("name","xxxx");
            String name = (String) operations.get("name");
            System.out.println("name: " + name);
            System.out.println(operations.get("k1"));
            Boolean result = (Boolean) redisTemplate.execute(redisScript, Collections.singletonList("k1"),value);
            System.out.println(result);
        }else {
            System.out.println("有其他线程正在使用，请稍后");
        }
    }
}
