package com.csu.takeout.controller.manager;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.dto.OrderDispatchDTO;
import com.csu.takeout.entity.Dispatcher;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.DispatcherService;
import com.csu.takeout.service.ManagerOrderService;
import com.csu.takeout.vo.DispatcherIdVO;
import com.csu.takeout.vo.ManagerOrderVO;
import com.csu.takeout.vo.ManagerUnsendOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 —— 订单管理（未派发 / 派送中 / 已送达）
 */
@RestController
@RequestMapping("/api/manager")
@Tag(name = "管理端-订单管理")
@Slf4j
@RequiredArgsConstructor
public class ManagerOrderController {

    private final ManagerOrderService managerOrderService;
    private final DispatcherService dispatcherService;

    /**
     * 未派发订单列表
     *
     * <p>接口契约：除 {@code tabledata} 外还返回 {@code disp_range}
     * （配送员编号候选列表，供派发表单的下拉框使用）。</p>
     */
    @GetMapping("/unsend")
    @Operation(summary = "查询未派发订单", description = "同时返回 disp_range 供下拉选择")
    public TableResult<ManagerUnsendOrderVO> listUnsend() {
        List<ManagerUnsendOrderVO> orders = managerOrderService.listUnsend();
        List<DispatcherIdVO> dispRange = dispatcherService.listAll().stream()
                .map(dispatcher -> new DispatcherIdVO(dispatcher.getDispatcherId()))
                .toList();

        TableResult<ManagerUnsendOrderVO> result = TableResult.success(orders);
        result.setDispRange(dispRange);
        return result;
    }

    /**
     * 派发订单
     *
     * <p>接口契约：{@code {"status": 200, "msg": "成功派发"}}。
     * 请求体字段为 {@code order_id}、{@code dispatcher_id}、{@code deliver_time}；
     * 写入物流记录后，订单状态由数据库触发器置为派送中。</p>
     */
    @PostMapping("/unsend")
    @Operation(summary = "派发订单")
    public TableResult<String> dispatchOrder(@RequestBody OrderDispatchDTO dto) {
        log.info("派发订单：orderId={}, dispId={}", dto.getOrderId(), dto.getDispatcherId());
        managerOrderService.dispatch(dto);
        return TableResult.success(MessageConstant.DISPATCH_SUCCESS);
    }

    /**
     * 派送中订单列表（数据来自视图 sending_order，不返回 disp_phone）
     */
    @GetMapping("/sending")
    @Operation(summary = "查询派送中订单")
    public TableResult<ManagerOrderVO> listSending() {
        List<ManagerOrderVO> orders = managerOrderService.listSending();
        return TableResult.success(orders);
    }

    /**
     * 已送达订单列表（数据来自视图 sended_order，不返回 disp_phone）
     */
    @GetMapping("/sended")
    @Operation(summary = "查询已送达订单")
    public TableResult<ManagerOrderVO> listSended() {
        List<ManagerOrderVO> orders = managerOrderService.listSended();
        return TableResult.success(orders);
    }
}
