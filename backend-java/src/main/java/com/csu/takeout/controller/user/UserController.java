package com.csu.takeout.controller.user;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.context.BaseContext;
import com.csu.takeout.dto.PasswordChangeDTO;
import com.csu.takeout.dto.UserLoginDTO;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.result.UserLoginResult;
import com.csu.takeout.service.UserService;
import com.csu.takeout.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端 —— 登录与账号相关接口
 *
 * <p>路径统一为：{@code /api/user/**}</p>
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户端-登录与账号")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 用户登录
     *
     * <p>接口契约（四个字段平铺在响应体根层级，注意用的是 {@code code} 而不是 {@code status}）：</p>
     * <pre>
     * 成功：{"code": 200, "msg": "登录成功", "token": "&lt;jwt&gt;", "role": 0}
     * 失败：{"code": 1000, "msg": "用户名或密码错误"}
     * </pre>
     * <p>因此这里不套用 {@code Result.data} 包装，直接返回 {@link UserLoginVO}。</p>
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "成功返回 code=200 与 token、role；失败返回 code=1000")
    public UserLoginVO login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录：{}", userLoginDTO.getUserortel());

        UserLoginResult loginResult = userService.login(userLoginDTO);
        return UserLoginVO.builder()
                .code(loginResult.getStatus())
                .msg(loginResult.getMsg())
                .token(loginResult.getToken())
                .role(loginResult.getRole())
                .build();
    }

    /**
     * 修改密码
     *
     * <p>接口契约（该接口需要 token，手机号取自 JWT）：</p>
     * <pre>
     * 原密码错误：{"status": 1000, "msg": "原始密码错误"}
     * 修改成功：  {"status": 200,  "msg": "修改成功"}
     * </pre>
     */
    @PostMapping("/pwd_chg")
    @Operation(summary = "修改密码", description = "校验原密码，成功返回 status=200；原密码错误返回 status=1000")
    public TableResult<String> changePassword(@RequestBody PasswordChangeDTO passwordChangeDTO) {
        String telephone = BaseContext.getCurrentTelephone();
        log.info("修改密码，手机号：{}", telephone);

        boolean success = userService.changePassword(
                telephone, passwordChangeDTO.getOldPwd(), passwordChangeDTO.getNewPwd());
        if (!success) {
            return TableResult.error(MessageConstant.OLD_PASSWORD_ERROR);
        }
        return TableResult.success(MessageConstant.PASSWORD_EDIT_SUCCESS);
    }
}
