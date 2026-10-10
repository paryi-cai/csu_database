package com.csu.takeout.mapper;

import com.csu.takeout.dto.UserMsgUpdateDTO;
import com.csu.takeout.entity.UserMsg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户详细信息持久层
 */
@Mapper
public interface UserMsgMapper {

    /**
     * 按手机号查询用户详细信息（对应 Flask 版
     * {@code select * from user_msg where phone=?}）
     *
     * @param phone 手机号（取自 JWT）
     * @return 用户信息，不存在时返回 null
     */
    UserMsg getByPhone(@Param("phone") String phone);

    /**
     * 按手机号修改个人信息（可改 user_name / real_name / sex / age / mail）。
     *
     * <p>刻意不更新 {@code phone} 与 {@code id}：前者是本查询的定位键，
     * 后者是 {@code user.id} 的外键关联列，改动都会让记录失联。</p>
     *
     * @param phone 手机号（取自 JWT）
     * @param dto   待更新的字段
     * @return 受影响行数（0 表示该手机号没有明细记录）
     */
    int updateByPhone(@Param("phone") String phone, @Param("dto") UserMsgUpdateDTO dto);
}
