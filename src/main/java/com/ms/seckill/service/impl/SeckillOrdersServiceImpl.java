package com.ms.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ms.seckill.mapper.SeckillOrdersMapper;
import com.ms.seckill.pojo.SeckillOrders;
import com.ms.seckill.pojo.User;
import com.ms.seckill.service.ISeckillOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author ms
 * @since 2022-09-01
 */
@Service
public class SeckillOrdersServiceImpl extends ServiceImpl<SeckillOrdersMapper, SeckillOrders> implements ISeckillOrdersService {
    @Autowired
    private SeckillOrdersMapper seckillOrdersMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取秒杀结果
     * orderId:成功   -1:失败   0：排队中
     * @param user
     * @param goodsId
     * @return
     */
    @Override
    public Long getResult(User user, Long goodsId) {
        QueryWrapper<SeckillOrders> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",user.getId());
        wrapper.eq("goods_id",goodsId);
        SeckillOrders seckillOrders = seckillOrdersMapper.selectOne(wrapper);
        if (null != seckillOrders) {
            return seckillOrders.getOrderId();
        } else if (redisTemplate.hasKey("isStockEmpty:" + goodsId)) {
            // 秒杀失败
            return -1L;
        }else {
            return 0L;
        }
    }
}
