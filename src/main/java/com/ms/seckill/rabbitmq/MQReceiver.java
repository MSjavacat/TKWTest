package com.ms.seckill.rabbitmq;

import com.ms.seckill.pojo.SeckillOrders;
import com.ms.seckill.pojo.User;
import com.ms.seckill.service.IGoodsService;
import com.ms.seckill.service.IOrderService;
import com.ms.seckill.utils.JSONUtil;
import com.ms.seckill.vo.GoodsVo;
import com.ms.seckill.vo.RespBean;
import com.ms.seckill.vo.RespBeanEnum;
import com.ms.seckill.vo.SeckillMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author MS
 * @create 2022-09-04-9:26
 */
@Service
@Slf4j
public class MQReceiver {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IOrderService orderService;

    /**
     * 下单操作
     * @param message
     */
    @RabbitListener(queues = "seckillQueue")
    public void receive(String message){
        log.info("接受消息:{}",message);
        SeckillMessage seckillMessage = JSONUtil.jsonToPojo(message, SeckillMessage.class);
        Long goodId = seckillMessage.getGoodId();
        User user = seckillMessage.getUser();
        GoodsVo goodsVo = goodsService.findGoodsVoById(goodId);
        // 判断库存
        if (goodsVo.getStockCount() < 0) {
            return;
        }
        // 判断该用户是否重复抢购
        // 解决超卖：user_id,goods_id创建唯一联合索引
        SeckillOrders seckillOrder = (SeckillOrders) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodId);
        if (seckillOrder != null) {
            // 重复抢购
            return;
        }
        // 下单
        orderService.seckill(user, goodsVo);
    }

}
