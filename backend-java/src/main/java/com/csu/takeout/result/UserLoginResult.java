package com.csu.takeout.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录业务结果（内部传输对象，不是响应体）。
 *
 * <p>Flask 版的登录接口把 code/msg/token/role 平铺在响应体根层级上，
 * 因此这里先由 service 层产出"状态 + 数据"，再由 controller 摊平成
 * {@code {"code":..., "msg":..., "token":..., "role":...}}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResult implements Serializable {

    /** 200 成功，1000 用户名或密码错误 */
    private Integer status;

    /** 提示信息 */
    private String msg;

    /** 登录成功时的 JWT 令牌 */
    private String token;

    /** 登录成功时的角色：0 用户端，1 管理端 */
    private Integer role;

    public boolean isSuccess() {
        return Integer.valueOf(200).equals(status);
    }
}
