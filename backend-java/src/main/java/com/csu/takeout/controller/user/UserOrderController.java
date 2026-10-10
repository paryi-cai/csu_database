package com.csu.takeout.controller.user;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.context.BaseContext;
import com.csu.takeout.dto.OrderDeleteDTO;
import com.csu.takeout.dto.OrderSubmitDTO;
import com.csu.takeout.dto.OrderUpdateDTO;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.OrderService;
import com.csu.takeout.vo.SendingOrderVO;
import com.csu.takeout.vo.UnsendOrderVO;
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
 * 用户端 —— 订单相关接口
 *
 * <p>这三个接口在 Flask 版中同时声明了多个 HTTP 方法，但只有部分方法实现了业务分支：</p>
 * <ul>
 *   <li>{@code /api/user/unsend}：GET / POST / DELETE 均已实现；</li>
 *   <li>{@code /api/user/sending}、{@code /api/user/sended}：只实现了 GET，
 *       前端也只调用 GET，因此这里同样只暴露 GET（不臆造未实现的行为）。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户端-订单")
@Slf4j
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;

    /**
     * 下单
     *
     * <p>Flask 契约：{@code {"status": 200, "msg": "成功下单"}}。
     * 顾客手机号取自 JWT 载荷，请求体中<b>不</b>包含 cons_phone。</p>
     */
    @PostMapping("/addorder")
    @Operation(summary = "下单")
    public TableResult<String> addOrder(@RequestBody OrderSubmitDTO orderSubmitDTO) {
        String consPhone = BaseContext.getCurrentTelephone();
        log.info("用户下单：shop={}, phone={}", orderSubmitDTO.getShopName(), consPhone);
        orderService.addOrder(consPhone, orderSubmitDTO);
        return TableResult.success(MessageConstant.ORDER_SUCCESS);
    }

    /**
     * 查询未派发订单
     */
    @GetMapping("/unsend")
    @Operation(summary = "查询未派发订单")
    public TableResult<UnsendOrderVO> listUnsend() {
        List<UnsendOrderVO> list = orderService.listUnsend(BaseContext.getCurrentTelephone());
        return TableResult.success(list);
    }

    /**
     * 修改未派发订单的收货信息。
     *
     * <p>手机号取自 JWT 并下推到 SQL 作为归属条件，因此只能改自己的订单；
     * 一行都没改到时返回业务失败而不是"修改成功"。</p>
     */
    @PostMapping("/unsend")
    @Operation(summary = "修改未派发订单")
    public TableResult<String> updateUnsend(@RequestBody OrderUpdateDTO orderUpdateDTO) {
        boolean success = orderService.updateUnsend(BaseContext.getCurrentTelephone(), orderUpdateDTO);
        if (!success) {
            log.warn("修改订单失败：订单不存在或不属于当前用户，orderId={}", orderUpdateDTO.getOrderId());
            return TableResult.error(MessageConstant.ORDER_NOT_FOUND);
        }
        return TableResult.success(MessageConstant.ORDER_UPDATE_SUCCESS);
    }

    /**
     * 取消（删除）未派发订单
     *
     * <p>请求体字段名是 {@code delete_id}（与 Flask 版一致），前端通过 axios 的
     * {@code { data: {...} }} 以 DELETE 请求体发送。</p>
     *
     * <p>同样带归属校验：不能删除他人订单（否则会误改别人店铺的销量统计）。</p>
     */
    @DeleteMapping("/unsend")
    @Operation(summary = "取消订单")
    public TableResult<String> deleteUnsend(@RequestBody OrderDeleteDTO orderDeleteDTO) {
        boolean success = orderService.deleteOrder(BaseContext.getCurrentTelephone(), orderDeleteDTO.getDeleteId());
        if (!success) {
            log.warn("取消订单失败：订单不存在或不属于当前用户，orderId={}", orderDeleteDTO.getDeleteId());
            return TableResult.error(MessageConstant.ORDER_NOT_FOUND);
        }
        return TableResult.success(MessageConstant.ORDER_DELETE_SUCCESS);
    }

    /**
     * 查询派送中订单（数据来自数据库视图 sending_order）
     */
    @GetMapping("/sending")
    @Operation(summary = "查询派送中订单")
    public TableResult<SendingOrderVO> listSending() {
        List<SendingOrderVO> list = orderService.listSending(BaseContext.getCurrentTelephone());
        return TableResult.success(list);
    }

    /**
     * 查询已送达订单（数据来自数据库视图 sended_order）
     */
    @GetMapping("/sended")
    @Operation(summary = "查询已送达订单")
    public TableResult<SendingOrderVO> listSended() {
        List<SendingOrderVO> list = orderService.listSended(BaseContext.getCurrentTelephone());
        return TableResult.success(list);
    }
}
