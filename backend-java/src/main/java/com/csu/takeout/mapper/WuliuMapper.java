package com.csu.takeout.mapper;

import com.csu.takeout.entity.Wuliu;
import com.csu.takeout.vo.WuliuVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 物流记录持久层
 */
@Mapper
public interface WuliuMapper {

    /**
     * 按是否结束查询物流记录（对应 Flask 版
     * {@code select * from wuliu where ended=?}）
     *
     * @param ended 0 进行中、1 已完成
     */
    List<WuliuVO> listByEnded(@Param("ended") Integer ended);

    /**
     * 新增物流记录（派发订单）
     *
     * <p>插入会触发数据库触发器 {@code wuliu_insert}，把该订单的
     * {@code checked} 置为 1（派送中）——业务代码不得自行更新订单状态。</p>
     */
    int insert(Wuliu wuliu);
}
