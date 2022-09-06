package com.ms.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ms.seckill.annotation.AccessLimit;
import com.ms.seckill.pojo.Order;
import com.ms.seckill.pojo.SeckillOrders;
import com.ms.seckill.pojo.User;
import com.ms.seckill.rabbitmq.MQSender;
import com.ms.seckill.service.IGoodsService;
import com.ms.seckill.service.IOrderService;
import com.ms.seckill.service.ISeckillOrdersService;
import com.ms.seckill.utils.JSONUtil;
import com.ms.seckill.vo.GoodsVo;
import com.ms.seckill.vo.RespBean;
import com.ms.seckill.vo.RespBeanEnum;
import com.ms.seckill.vo.SeckillMessage;
import com.wf.captcha.ArithmeticCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author MS
 * @create 2022-09-01-19:38
 */
@Slf4j
@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrdersService seckillOrdersService;
    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;
    @Autowired
    private RedisScript<Long> script;

    private Map<Long, Boolean> goodsMap = new HashMap<>();

    /**
     * 获取秒杀地址
     * @param user
     * @param goodsId
     * @return
     */
    @AccessLimit(second = 5, maxCount = 5, needLogin = true)
    @RequestMapping("/path")
    @ResponseBody
    public RespBean getPath(User user, Long goodsId, String captcha){
        if (user == null) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }
        boolean check = orderService.checkCaptcha(user,goodsId,captcha);
        if (!check) {
            return RespBean.error(RespBeanEnum.ERROR);
        }
        String path = orderService.createPath(user, goodsId);
        return RespBean.success(path);
    }

    /**
     * 秒杀
     * @param path
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/{path}/doSeckill",method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSeckill(@PathVariable String path, User user, Long goodsId){
        // 判断登录
        if (user == null) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }

        ValueOperations valueOperations = redisTemplate.opsForValue();

        // 秒杀地址判断，如果和redis中生成的一致，说明正确
        boolean check = orderService.checkPath(path, user, goodsId);

        if (!check) {
            return RespBean.error(RespBeanEnum.ERROR);
        }

        // 判断库存
        if (goodsMap.get(goodsId)) {
            // 返回true，说明库存为空
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        // 判断该用户是否重复抢购
        // 解决超卖：user_id,goods_id创建唯一联合索引
        SeckillOrders seckillOrder = (SeckillOrders) valueOperations.get("order:" + user.getId() + ":" + goodsId);

        if (seckillOrder != null) {
            // 重复抢购
            return RespBean.error(RespBeanEnum.ERROR);
        }

        // 预减库存(原子性操作)
        // Long stock = redisTemplate.opsForValue().decrement("seckillGoods:" + goodsId);

        Long stock = ((Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST));

        if (stock < 0) {
            // 将内存标记置为true，表示库存没有了，不和redis通信
            goodsMap.put(goodsId,true);
            // 库存置为0
            // redisTemplate.opsForValue().increment("seckillGoods:"+goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }

        SeckillMessage message = new SeckillMessage(user, goodsId);
        mqSender.sendSeckillMessage(JSONUtil.objectToJson(message));

        return RespBean.success(0);
    }

    /**
     * 生成验证码
     * @param user
     * @param goodsId
     * @param response
     */
    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public void verifyCode(User user, Long goodsId, HttpServletResponse response){
        if (user == null) {
            throw new RuntimeException("请先登录");
        }

        // 设置请求头为输出图片类型
        response.setContentType("image/gif");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        // 将验证码设置到缓存中，过期时间300s
        redisTemplate.opsForValue().set("captcha:"+user.getId()+":"+goodsId,captcha.text(),300, TimeUnit.SECONDS);
        try {
            captcha.out(response.getOutputStream());
        }catch (IOException e){
            log.error("验证码生成失败",e.getMessage());
        }
    }

    /**
     * 客户端轮询获取秒杀结果
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/result",method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId){
        if (user == null) {
            return RespBean.error(RespBeanEnum.LOGIN_ERROR);
        }
        Long orderId = seckillOrdersService.getResult(user, goodsId);
        return RespBean.success(orderId);
    }

    /**
     * 容器初始化的时候将库存设置到redis中
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> list = goodsService.findGoodsVo();
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            redisTemplate.opsForValue().set("seckillGoods:"+list.get(i).getId(),list.get(i).getStockCount());
            // 内存标记，false表示有库存
            goodsMap.put(list.get(i).getId(),false);
        }
    }

    @RequestMapping("/doSeckill2")
    public String doSeckill2(Model model, User user, Long goodsId){
        // 判断登录
        if (user == null) {
            return "login";
        }
        model.addAttribute("user",user);
        // 判断该商品库存
        GoodsVo goodsVo = goodsService.findGoodsVoById(goodsId);
        if (goodsVo.getStockCount() == 0) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "seckillFail";
        }
        // 判断该用户是否重复抢购
        QueryWrapper<SeckillOrders> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id",user.getId());
        wrapper.eq("goods_id",goodsId);
        SeckillOrders seckillOrder = seckillOrdersService.getOne(wrapper);

        if (seckillOrder != null) {
            // 重复抢购
            model.addAttribute("errmsg",RespBeanEnum.REPEATE_ERROR.getMessage());
            return "seckillFail";
        }

        Order order = orderService.seckill(user,goodsVo);
        model.addAttribute("goods",goodsVo);
        model.addAttribute("order",order);
        return "orderDetail";
    }
}
