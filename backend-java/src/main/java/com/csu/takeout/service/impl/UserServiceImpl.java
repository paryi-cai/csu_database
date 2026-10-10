package com.csu.takeout.service.impl;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.dto.JwtUserData;
import com.csu.takeout.dto.UserLoginDTO;
import com.csu.takeout.entity.User;
import com.csu.takeout.mapper.UserMapper;
import com.csu.takeout.properties.JwtProperties;
import com.csu.takeout.result.UserLoginResult;
import com.csu.takeout.service.UserService;
import com.csu.takeout.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户相关业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtProperties jwtProperties;

    /**
     * 用户登录
     *
     * <p>与 Flask 版一致：先对手机号与密码做 {@code .strip()}（去首尾空白），
     * 再按 手机号 + 密码 精确匹配查询 user 表；查询不到返回 code=1000。</p>
     *
     * <p>对齐说明：Flask 的 app.py 写的是
     * {@code userortel = request.json.get("userortel").strip()}，
     * 即入参确实会去空白。因此这里必须同样 trim，否则"带首尾空格的账号密码"
     * 在 Flask 能登录、在新后端会失败。</p>
     */
    @Override
    public UserLoginResult login(UserLoginDTO userLoginDTO) {
        String telephone = trim(userLoginDTO.getUserortel());
        String password = trim(userLoginDTO.getPassword());

        User user = userMapper.getByTelephoneAndPassword(telephone, password);
        if (user == null) {
            log.info("登录失败，手机号或密码错误：{}", telephone);
            return UserLoginResult.builder()
                    .status(1000)
                    .msg(MessageConstant.PASSWORD_ERROR)
                    .build();
        }

        // 生成 token：载荷结构与 Flask 版 auth.py 完全一致
        JwtUserData userData = JwtUserData.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .telephone(user.getTelephone())
                .build();
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(), jwtProperties.getTtl(), jwtProperties.getIssuer(), userData);

        log.info("登录成功，用户：{}，角色：{}", user.getUsername(), user.getRole());
        return UserLoginResult.builder()
                .status(200)
                .msg(MessageConstant.LOGIN_SUCCESS)
                .token(token)
                .role(user.getRole())
                .build();
    }

    /**
     * 修改密码
     *
     * <p>与 Flask 版一致：先用 手机号 + 原密码 校验身份，校验通过后按手机号更新密码；
     * 不校验新旧密码是否相同，也不影响已签发的 token。</p>
     *
     * <p>Flask 版该接口未对 {@code new_pwd} / {@code old_pwd} 做 {@code .strip()}
     * （直接取 {@code request.json.get(...)}），这里保持一致，不额外 trim。</p>
     */
    @Override
    @Transactional
    public boolean changePassword(String telephone, String oldPwd, String newPwd) {
        User user = userMapper.getByTelephoneAndPassword(telephone, oldPwd);
        if (user == null) {
            return false;
        }
        userMapper.updatePasswordByTelephone(telephone, newPwd);
        return true;
    }

    /**
     * 复刻 Flask 的 {@code str.strip()} 语义：去首尾空白，{@code null} 原样返回。
     *
     * <p>注意与 Java 的 {@link String#trim()} 的区别：{@code strip()} 按 Unicode 空白判定，
     * {@code trim()} 只处理 ≤ U+0020 的字符。本项目入参为手机号与密码，两者等价；
     * 这里用 {@code strip()} 以与 Flask 严格一致。</p>
     */
    private String trim(String value) {
        return value == null ? null : value.strip();
    }
}
