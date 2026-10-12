package com.csu.takeout.constant;

/**
 * 信息提示常量类
 *
 * <p>取值与前端长期展示的文案逐字一致，前端会直接展示这些字符串。</p>
 */
public class MessageConstant {

    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String PASSWORD_ERROR = "用户名或密码错误";

    public static final String ORDER_SUCCESS = "成功下单";
    public static final String ORDER_UPDATE_SUCCESS = "修改成功";
    public static final String ORDER_DELETE_SUCCESS = "删除成功";

    /**
     * 改单/取消订单时订单不存在**或不属于当前用户**。
     *
     * <p>两种情况刻意合并成同一句话：不区分它们可以避免"订单号存在性探测"
     * （否则调用方能通过不同提示枚举出别人有哪些订单号）。</p>
     */
    public static final String ORDER_NOT_FOUND = "订单不存在或不属于当前用户";

    public static final String OLD_PASSWORD_ERROR = "原始密码错误";
    public static final String PASSWORD_EDIT_SUCCESS = "修改成功";

    /** 个人信息编辑（PUT /api/user/usermsg/edit） */
    public static final String PROFILE_UPDATE_SUCCESS = "修改成功";

    /** 个人信息编辑：请求体里没有任何可改字段 */
    public static final String NOTHING_TO_UPDATE = "没有需要修改的字段";

    /** 个人信息编辑：user_msg 中查不到该手机号的明细记录 */
    public static final String USER_MSG_NOT_FOUND = "用户信息不存在";

    /** 个人信息编辑：必填字段被显式传成空白 */
    public static final String PROFILE_FIELD_BLANK = "用户名、真实姓名、性别、年龄、邮箱都不能为空";

    public static final String SHOP_ADD_SUCCESS = "添加成功";
    public static final String SHOP_UPDATE_SUCCESS = "修改成功";
    public static final String SHOP_DELETE_SUCCESS = "删除成功";
    public static final String SHOP_ALREADY_EXISTS = "该店铺已存在";

    public static final String SERVER_ADD_SUCCESS = "添加成功";
    public static final String SERVER_DELETE_SUCCESS = "解雇成功";
    public static final String SERVER_ALREADY_EXISTS = "该编号已存在";

    public static final String DISPATCHER_ADD_SUCCESS = "添加成功";
    public static final String DISPATCHER_DELETE_SUCCESS = "解雇成功";
    public static final String DISPATCHER_ALREADY_EXISTS = "该编号已存在";

    public static final String DISPATCH_SUCCESS = "成功派发";

    /** 数据统计：店铺下钻时店铺名不存在 */
    public static final String SHOP_NOT_FOUND = "该店铺不存在";

    /** 异常告警：测试邮件已发送 */
    public static final String ALERT_TEST_SENT = "测试告警已发送";

    /** 异常告警：未启用或发送失败（含 SMTP 未配置） */
    public static final String ALERT_NOT_SENT = "告警未启用或发送失败：请检查 csu.alert.* 与 spring.mail.* 配置";

    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String UNKNOWN_ERROR = "未知错误";
}
