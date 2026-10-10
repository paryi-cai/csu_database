package com.csu.takeout.controller.user;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.context.BaseContext;
import com.csu.takeout.dto.UserMsgPwdDTO;
import com.csu.takeout.dto.UserMsgUpdateDTO;
import com.csu.takeout.result.DataResult;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.UserMsgService;
import com.csu.takeout.vo.UserMsgVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户端 —— 个人信息（查看 / 修改资料 / 修改密码）
 *
 * <p>本类的三个接口都在 {@code WebMvcConfiguration.JWT_PATH_PATTERNS} 内，
 * 手机号一律取自 {@code BaseContext}（由 JWT 拦截器写入），请求体不传手机号。</p>
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户端-个人信息")
@Slf4j
@RequiredArgsConstructor
public class UserMsgController {

    private final UserMsgService userMsgService;

    /**
     * 查询个人信息
     *
     * <p>Flask 契约（注意是 {@code status} + {@code data}，且 data 为<b>对象</b>而非数组）：</p>
     * <pre>
     * {"status": 200, "data": {"real_name": "...", "sex": "...", "age": 20,
     *                          "mail": "...", "phone": "...", "user_name": "..."}}
     * </pre>
     */
    @GetMapping("/usermsg")
    @Operation(summary = "查询个人信息")
    public DataResult<UserMsgVO> getUserMsg() {
        UserMsgVO userMsg = userMsgService.getByPhone(BaseContext.getCurrentTelephone());
        return DataResult.success(userMsg);
    }

    /**
     * 修改个人信息（用户名 / 真实姓名 / 性别 / 年龄 / 邮箱）。
     *
     * <pre>
     * 成功：         {"status": 200,  "msg": "修改成功"}
     * 无记录：       {"status": 1000, "msg": "用户信息不存在"}
     * 字段全空：     {"status": 1000, "msg": "没有需要修改的字段"}
     * 必填字段为空： {"status": 1000, "msg": "用户名、真实姓名、性别、年龄、邮箱都不能为空"}
     * </pre>
     *
     * <p>用 {@code PUT} + {@code /usermsg/edit} 而不是复用 POST {@code /api/user/usermsg}：
     * Flask 版该路径的 POST 有装饰器但函数体里只实现了 GET 分支（POST 会落到隐式
     * {@code return None} → 500）。契约里"既有实现"只包含已实现的行为，
     * 因此不去"补全"一个原本会报错的 POST，而是新增一个语义明确的路径。</p>
     *
     * <p>请求体<b>不含 phone</b>，联系电话在页面上是只读项（它是查询键，见 DTO 注释）。</p>
     *
     * <p><b>为什么要把缺省字段用现有记录补齐</b>：{@code user_msg} 的这 5 列都是 NOT NULL，
     * 而 UPDATE 语句会把这 5 列全部写一遍。若客户端只传其中一个字段（例如只改年龄），
     * 未传的列就会被写 NULL、直接撞数据库约束（表现为 200 + "未知错误"，且事务回滚、
     * 数据不变——不致命但提示毫无信息量）。这里先取当前记录合并，保证下发的是完整值，
     * 于是"部分字段提交"与"整表提交"行为一致；若某列确实被显式传成空白，则明确报错。</p>
     */
    @PutMapping("/usermsg/edit")
    @Operation(summary = "修改个人信息", description = "可改 user_name / real_name / sex / age / mail；phone 只读")
    public TableResult<String> updateUserMsg(@RequestBody UserMsgUpdateDTO dto) {
        String phone = BaseContext.getCurrentTelephone();

        // 1. 取当前记录（同时兼作"记录是否存在"的判断）
        UserMsgVO current = userMsgService.getByPhone(phone);
        if (current == null) {
            log.warn("修改个人信息失败：user_msg 中不存在手机号 {} 的记录", phone);
            return TableResult.error(MessageConstant.USER_MSG_NOT_FOUND);
        }

        // 2. 请求体里什么都没带：没有任何要改的，直接拒绝（避免下发一次纯原值的 UPDATE）
        if (isAllBlank(dto)) {
            log.warn("修改个人信息：请求体没有任何可改字段，已拒绝");
            return TableResult.error(MessageConstant.NOTHING_TO_UPDATE);
        }

        // 3. 用现有值补齐未提供的字段（null 视为"未提供"，不是"清空"）
        mergeMissingFromCurrent(dto, current);

        // 4. 补齐后仍有空白字段：说明客户端把某个 NOT NULL 列显式传成了空
        //    （这里只需 hasBlank：isAllBlank 是它的子集，合并补齐后已被完全覆盖）
        if (hasBlank(dto)) {
            log.warn("修改个人信息：存在空白必填字段，已拒绝");
            return TableResult.error(MessageConstant.PROFILE_FIELD_BLANK);
        }

        // 5. 下发（此时 5 个字段都是完整值，不可能撞 NOT NULL 约束）
        boolean success = userMsgService.updateByPhone(phone, dto);
        if (!success) {
            // 竞态兜底：取记录与更新之间该行被删掉了
            log.warn("修改个人信息失败：更新影响行数为 0，phone={}", phone);
            return TableResult.error(MessageConstant.USER_MSG_NOT_FOUND);
        }
        return TableResult.success(MessageConstant.PROFILE_UPDATE_SUCCESS);
    }

    /**
     * 5 个可改字段是否全为空（null 或纯空白）。
     *
     * <p>注意与 {@link #hasBlank} 的分工，两步顺序不能换：</p>
     * <ul>
     *   <li>合并<b>之前</b>用它判断"客户端什么都没提交"，此时"全空"才是有效信息；</li>
     *   <li>合并<b>之后</b>只需 {@code hasBlank}——补齐过的字段不可能仍为 null，
     *       此时"全空"已被"存在空白"完全覆盖，再判一次是死代码。</li>
     * </ul>
     */
    private boolean isAllBlank(UserMsgUpdateDTO dto) {
        return isBlank(dto.getUserName())
                && isBlank(dto.getRealName())
                && isBlank(dto.getSex())
                && dto.getAge() == null
                && isBlank(dto.getMail());
    }

    /** 补齐后是否仍存在空白必填字段 */
    private boolean hasBlank(UserMsgUpdateDTO dto) {
        return isBlank(dto.getUserName())
                || isBlank(dto.getRealName())
                || isBlank(dto.getSex())
                || dto.getAge() == null
                || isBlank(dto.getMail());
    }

    /** 把请求体里未提供的字段用现有记录补上（显式传空字符串不算"未提供"，会被下一步拦下） */
    private void mergeMissingFromCurrent(UserMsgUpdateDTO dto, UserMsgVO current) {
        if (dto.getUserName() == null) dto.setUserName(current.getUserName());
        if (dto.getRealName() == null) dto.setRealName(current.getRealName());
        if (dto.getSex() == null) dto.setSex(current.getSex());
        if (dto.getAge() == null) dto.setAge(current.getAge());
        if (dto.getMail() == null) dto.setMail(current.getMail());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 个人信息页内的修改密码。
     *
     * <p>响应形状与既有 {@code POST /api/user/pwd_chg} 保持一致：
     * {@code {"status": 200, "msg": "修改成功"}} / 原密码错误 {@code {"status": 1000, "msg": "原始密码错误"}}。</p>
     *
     * <p>前端只有这一个改密入口（原先独立的「修改密码」菜单页已移除）；
     * 既有的 {@code POST /api/user/pwd_chg} 仍然保留 —— 它属于既有接口，
     * 契约测试继续覆盖它，只是界面上不再调用。</p>
     */
    @PutMapping("/usermsg/pwd")
    @Operation(summary = "修改密码（个人信息页）", description = "语义与 POST /api/user/pwd_chg 完全一致")
    public TableResult<String> changePassword(@RequestBody UserMsgPwdDTO dto) {
        String phone = BaseContext.getCurrentTelephone();
        boolean success = userMsgService.changePassword(phone, dto.getOldPwd(), dto.getNewPwd());
        if (!success) {
            return TableResult.error(MessageConstant.OLD_PASSWORD_ERROR);
        }
        return TableResult.success(MessageConstant.PASSWORD_EDIT_SUCCESS);
    }

    /**
     * 供前端渲染表单用的字段可编辑性说明（只读元数据，便于以后加字段时前后端一致）。
     *
     * <p>返回 {@code {"status":200, "data":{"editable":[...], "readonly":["phone"]}}}。
     * 前端当前按固定字段渲染，这个接口是为了让"哪些能改"这件事有单一事实来源。</p>
     */
    @GetMapping("/usermsg/editable")
    @Operation(summary = "个人信息字段可编辑性", description = "哪些字段可改、哪些只读")
    public Map<String, Object> editableFields() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("editable", new String[]{"user_name", "real_name", "sex", "age", "mail"});
        data.put("readonly", new String[]{"phone"});
        data.put("readonly_reason", "phone 是 GET /api/user/usermsg 的查询键，可改会导致记录失联");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 200);
        body.put("data", data);
        return body;
    }
}
