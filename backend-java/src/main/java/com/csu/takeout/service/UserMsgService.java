package com.csu.takeout.service;

import com.csu.takeout.dto.UserMsgUpdateDTO;
import com.csu.takeout.vo.UserMsgVO;

/**
 * 用户详细信息业务接口
 */
public interface UserMsgService {

    /**
     * 按手机号查询用户详细信息
     *
     * @param phone 手机号（取自 JWT 载荷）
     * @return 用户信息，不存在时返回 null
     */
    UserMsgVO getByPhone(String phone);

    /**
     * 修改个人信息（可改 用户名 / 真实姓名 / 性别 / 年龄 / 邮箱）。
     *
     * @param phone 手机号（取自 JWT 载荷）
     * @param dto   待更新字段
     * @return 是否修改成功（false 表示该手机号没有明细记录）
     */
    boolean updateByPhone(String phone, UserMsgUpdateDTO dto);

    /**
     * 在个人信息页修改密码（复用与 /api/user/pwd_chg 相同的校验语义）。
     *
     * @param phone 手机号（取自 JWT 载荷）
     * @param oldPwd 原密码，必须与库中一致
     * @param newPwd 新密码
     * @return 是否修改成功（false 表示原密码错误）
     */
    boolean changePassword(String phone, String oldPwd, String newPwd);
}