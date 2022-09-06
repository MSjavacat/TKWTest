package com.ms.seckill.controller;


import com.ms.seckill.pojo.User;
import com.ms.seckill.rabbitmq.MQSender;
import com.ms.seckill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author ms
 * @since 2022-08-29
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @Autowired
    private MQSender sender;

    @RequestMapping("/info")
    @ResponseBody
    public RespBean getInfo(User user){
        return RespBean.success(user);
    }


}
