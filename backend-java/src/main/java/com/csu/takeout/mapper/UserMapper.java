package com.csu.takeout.mapper;

import com.csu.takeout.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户相关持久层
 */
@Mapper
public interface UserMapper {

    /**
     * 根据手机号与密码查询用户（对应 Flask 版
     * {@code select * from user where telephone = ? and password = ?}）
     *
     * @param telephone 手机号
     * @param password  密码
     * @return 用户，不存在时返回 null
     */
    User getByTelephoneAndPassword(@Param("telephone") String telephone, @Param("password") String password);

    /**
     * 修改密码
     *
     * @param telephone 手机号
     * @param password  新密码
     * @return 影响行数
     */
    int updatePasswordByTelephone(@Param("telephone") String telephone, @Param("password") String password);
}
