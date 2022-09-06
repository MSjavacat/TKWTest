package com.ms.seckill.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ms.seckill.pojo.Order;
import com.ms.seckill.pojo.User;
import com.ms.seckill.vo.GoodsVo;
import com.ms.seckill.vo.OrderDetailVo;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author ms
 * @since 2022-09-01
 */
public interface IOrderService extends IService<Order> {

    Order seckill(User user, GoodsVo goodsVo);

    OrderDetailVo detail(Long orderId);

    String createPath(User user, Long goodsId);

    boolean checkPath(String path, User user, Long goodsId);

    boolean checkCaptcha(User user, Long goodsId, String captcha);
}
