package com.csu.takeout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * JWT 载荷中 data 声明的载体。
 *
 * <p>字段与 默认配置 中 {@code user} 字典完全一致。注意其中包含密码，
 * 这是既有契约的行为，为保证 token 结构完全兼容而保留（属于已知安全隐患，
 * 详见 README 的"说明"章节）。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserData implements Serializable {

    private Integer id;
    private String username;
    private String password;
    private String telephone;

    /**
     * 由 JWT claims 中的 data Map 还原（幂等，兼容 JSON 反序列化后 Integer/String 的类型差异）
     */
    public static JwtUserData fromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        return JwtUserData.builder()
                .id(toInteger(map.get("id")))
                .username(toStringValue(map.get("username")))
                .password(toStringValue(map.get("password")))
                .telephone(toStringValue(map.get("telephone")))
                .build();
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
