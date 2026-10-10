package com.csu.takeout.service.impl;

import com.csu.takeout.dto.UserMsgUpdateDTO;
import com.csu.takeout.entity.UserMsg;
import com.csu.takeout.mapper.UserMsgMapper;
import com.csu.takeout.service.UserMsgService;
import com.csu.takeout.service.UserService;
import com.csu.takeout.vo.UserMsgVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户详细信息业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserMsgServiceImpl implements UserMsgService {

    private final UserMsgMapper userMsgMapper;

    /**
     * 密码修改复用 {@link UserService#changePassword}，而不是在这里再写一遍。
     *
     * <p>原因：改密码实际写的是 {@code user.password}（不是 user_msg 表），
     * 而"用手机号 + 原密码校验身份"这套语义属于用户账号域；
     * 直接复用可以保证个人信息页与 /api/user/pwd_chg 两条入口行为完全一致。</p>
     */
    private final UserService userService;

    @Override
    public UserMsgVO getByPhone(String phone) {
        UserMsg userMsg = userMsgMapper.getByPhone(phone);
        if (userMsg == null) {
            // 既有接口此处会因 data[0] 越界而抛出 IndexError（返回 500）；
            // 新实现返回 data=null，避免把异常抛给前端，正常数据下响应完全一致。
            log.warn("user_msg 中不存在手机号 {} 的记录", phone);
            return null;
        }
        return toVO(userMsg);
    }

    /**
     * 修改个人信息。
     *
     * <p>定位方式与查询一致（都按 {@code phone}），这样"查得到就一定改得到"；
     * 影响行数为 0 时返回 false，由 Controller 转成带 msg 的业务失败响应。</p>
     */
    @Override
    @Transactional
    public boolean updateByPhone(String phone, UserMsgUpdateDTO dto) {
        int rows = userMsgMapper.updateByPhone(phone, dto);
        log.info("修改个人信息：phone={}, 影响行数={}", phone, rows);
        return rows > 0;
    }

    @Override
    public boolean changePassword(String phone, String oldPwd, String newPwd) {
        return userService.changePassword(phone, oldPwd, newPwd);
    }

    /** 实体 -> 响应对象（响应键由全局下划线命名策略生成） */
    private UserMsgVO toVO(UserMsg userMsg) {
        return UserMsgVO.builder()
                .realName(userMsg.getRealName())
                .sex(userMsg.getSex())
                .age(userMsg.getAge())
                .mail(userMsg.getMail())
                .phone(userMsg.getPhone())
                .userName(userMsg.getUserName())
                .build();
    }
}
