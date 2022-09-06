package com.ms.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ms.seckill.mapper.OrderMapper;
import com.ms.seckill.pojo.Order;
import com.ms.seckill.pojo.SeckillGoods;
import com.ms.seckill.pojo.SeckillOrders;
import com.ms.seckill.pojo.User;
import com.ms.seckill.service.IGoodsService;
import com.ms.seckill.service.IOrderService;
import com.ms.seckill.service.ISeckillGoodsService;
import com.ms.seckill.service.ISeckillOrdersService;
import com.ms.seckill.utils.MD5Util;
import com.ms.seckill.utils.UUIDUtils;
import com.ms.seckill.vo.GoodsVo;
import com.ms.seckill.vo.OrderDetailVo;
import com.ms.seckill.vo.RespBean;
import com.ms.seckill.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ms
 * @since 2022-09-01
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {
    @Autowired
    private ISeckillGoodsService seckillGoodsService;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ISeckillOrdersService seckillOrdersService;
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 减库存
     * @param user
     * @param goodsVo
     * @return
     */
    @Transactional
    @Override
    public Order seckill(User user, GoodsVo goodsVo) {
        // 减库存
        QueryWrapper<SeckillGoods> wrapper = new QueryWrapper<>();
        wrapper.eq("goods_id",goodsVo.getId());
        SeckillGoods seckillGoods = seckillGoodsService.getOne(wrapper);
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
        // 更新库存
        seckillGoodsService.update(
                new UpdateWrapper<SeckillGoods>().setSql("stock_count = " +
                "stock_count-1").eq("goods_id",goodsVo.getId())
                .gt("stock_count",0)
        );

        if(seckillGoods.getStockCount() < 1){
            // 标记内存为空
            redisTemplate.opsForValue().set("isStockEmpty:"+goodsVo.getId(),"0");
            return null;
        }

        // 生成订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setGoodsId(seckillGoods.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goodsVo.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);

        // 生成秒杀订单
        SeckillOrders seckillOrders = new SeckillOrders();
        seckillOrders.setUserId(user.getId());
        seckillOrders.setOrderId(order.getId());
        seckillOrders.setGoodsId(seckillGoods.getId());
        seckillOrdersService.save(seckillOrders);

        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + seckillGoods.getId(), seckillOrders);

        return order;
    }

    @Override
    public OrderDetailVo detail(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("orderId is null");
        }
        Order order = orderMapper.selectById(orderId);
        GoodsVo goodsVo = goodsService.findGoodsVoById(order.getGoodsId());
        OrderDetailVo detail = new OrderDetailVo();
        detail.setOrder(order);
        detail.setGoodsVo(goodsVo);
        return detail;
    }

    @Override
    public String createPath(User user, Long goodsId) {
        String str = MD5Util.md5(UUIDUtils.uuid() + "123456");
        // 秒杀地址60s后过期
        redisTemplate.opsForValue().set("seckillPath:"+user.getId()+":"+goodsId,str,60, TimeUnit.SECONDS);
        return str;
    }

    /**
     * 校验秒杀地址
     * @param user
     * @param goodsId
     * @return
     */
    @Override
    public boolean checkPath(String path, User user, Long goodsId) {
        if (user == null) {
            return false;
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String redisPath = ((String) valueOperations.get("seckillPath:" + user.getId() + ":" + goodsId));
        return path.equals(redisPath);
    }

    /**
     * 校验验证码
     * @param user
     * @param goodsId
     * @param captcha
     * @return
     */
    @Override
    public boolean checkCaptcha(User user, Long goodsId, String captcha) {
        if (user == null) {
            throw new RuntimeException("错误");
        }
        String redisCaptcha = ((String) redisTemplate.opsForValue().get("captcha:" + user.getId() + ":" + goodsId));
        return captcha.equals(redisCaptcha);
    }
}
