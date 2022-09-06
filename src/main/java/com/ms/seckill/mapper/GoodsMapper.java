package com.ms.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ms.seckill.pojo.Goods;
import com.ms.seckill.vo.GoodsVo;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author ms
 * @since 2022-09-01
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    List<GoodsVo> findGoodsVo();

    GoodsVo findGoodsVoById(Long goodsId);
}
