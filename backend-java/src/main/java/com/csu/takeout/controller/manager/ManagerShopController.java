package com.csu.takeout.controller.manager;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.dto.ManagerShopDTO;
import com.csu.takeout.dto.WantDeleteDTO;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.ManagerShopService;
import com.csu.takeout.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 —— 店铺管理
 *
 * <p>Flask 契约（该接口不校验 token）：POST 是同一个处理函数，按请求体的
 * {@code action} 字段区分 {@code add} 与 {@code change} 两种操作，因此这里同样
 * 只声明<b>一个</b> POST 方法（两个同路径的 @PostMapping 会导致映射冲突）。</p>
 */
@RestController
@RequestMapping("/api/manager/shop")
@Tag(name = "管理端-店铺管理")
@Slf4j
@RequiredArgsConstructor
public class ManagerShopController {

    private final ManagerShopService managerShopService;

    /** 店铺列表：与用户端 /api/user/shop 响应结构完全一致 */
    @GetMapping
    @Operation(summary = "查询店铺列表")
    public TableResult<ShopVO> listShop() {
        List<ShopVO> shops = managerShopService.listAll();
        return TableResult.success(shops);
    }

    /**
     * 新增 / 修改店铺
     *
     * <pre>
     * action=add    已存在：{"status":1000, "msg":"该店铺已存在"}
     *               成功：  {"status":200,  "msg":"添加成功"}
     * action=change 成功：  {"status":200,  "msg":"修改成功"}
     * </pre>
     */
    @PostMapping
    @Operation(summary = "新增或修改店铺", description = "请求体 action=add 新增，action=change 修改")
    public TableResult<String> addOrChangeShop(@RequestBody ManagerShopDTO dto) {
        Integer price = toInteger(dto.getPrice());
        Integer mSaleV = toInteger(dto.getMSaleV());
        if (price == null || mSaleV == null) {
            // 复刻 Flask 的 int(...) 转换失败场景
            return TableResult.error(MessageConstant.UNKNOWN_ERROR);
        }

        if ("add".equals(dto.getAction())) {
            boolean success = managerShopService.add(dto.getShopName(), price, mSaleV);
            return success
                    ? TableResult.success(MessageConstant.SHOP_ADD_SUCCESS)
                    : TableResult.error(MessageConstant.SHOP_ALREADY_EXISTS);
        }
        if ("change".equals(dto.getAction())) {
            managerShopService.change(dto.getShopName(), price, mSaleV);
            return TableResult.success(MessageConstant.SHOP_UPDATE_SUCCESS);
        }
        // Flask 版：action 既不是 add 也不是 change 时没有任何分支命中，返回 500。
        // 这里收敛为业务失败响应（HTTP 200 + msg），避免依赖异常；前端只会传 add/change。
        log.warn("未知的 action：{}", dto.getAction());
        return TableResult.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 删除店铺
     *
     * <p>请求体字段为 {@code want_delete}（店铺名）。若店铺被服务员表外键引用，
     * 数据库会拒绝删除。</p>
     */
    @DeleteMapping
    @Operation(summary = "删除店铺")
    public TableResult<String> deleteShop(@RequestBody WantDeleteDTO dto) {
        managerShopService.delete(dto.getWantDelete());
        return TableResult.success(MessageConstant.SHOP_DELETE_SUCCESS);
    }

    /** 复刻 Flask 的 int() 语义：字符串转整数，失败返回 null */
    private Integer toInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
