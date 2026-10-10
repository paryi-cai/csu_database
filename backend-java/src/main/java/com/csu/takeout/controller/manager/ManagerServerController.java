package com.csu.takeout.controller.manager;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.dto.ServerAddDTO;
import com.csu.takeout.dto.WantDeleteDTO;
import com.csu.takeout.entity.Server;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.ServerService;
import com.csu.takeout.service.ShopService;
import com.csu.takeout.vo.ShopNameVO;
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
 * 管理端 —— 服务员管理
 */
@RestController
@RequestMapping("/api/manager/server")
@Tag(name = "管理端-服务员管理")
@Slf4j
@RequiredArgsConstructor
public class ManagerServerController {

    private final ServerService serverService;
    private final ShopService shopService;

    /**
     * 服务员列表
     *
     * <p>接口契约：除 {@code tabledata} 外还返回 {@code shop_range}
     * （店铺名候选列表，供新增表单的下拉框使用）。</p>
     */
    @GetMapping
    @Operation(summary = "查询服务员列表", description = "同时返回 shop_range 供下拉选择")
    public TableResult<Server> listServer() {
        List<Server> servers = serverService.listAll();
        List<ShopNameVO> shopRange = shopService.listAll().stream()
                .map(shop -> new ShopNameVO(shop.getShopName()))
                .toList();

        TableResult<Server> result = TableResult.success(servers);
        result.setShopRange(shopRange);
        return result;
    }

    /**
     * 新增服务员
     *
     * <p>响应：编号已存在 → {@code {"status":1000,"msg":"该编号已存在"}}；
     * 成功 → {@code {"status":200,"msg":"添加成功"}}</p>
     */
    @PostMapping
    @Operation(summary = "新增服务员")
    public TableResult<String> addServer(@RequestBody ServerAddDTO dto) {
        boolean success = serverService.add(dto.getServiceId(), dto.getServiceName(), dto.getFastfoodShopName());
        return success
                ? TableResult.success(MessageConstant.SERVER_ADD_SUCCESS)
                : TableResult.error(MessageConstant.SERVER_ALREADY_EXISTS);
    }

    /** 解雇服务员（请求体字段 {@code want_delete} 为服务员编号） */
    @DeleteMapping
    @Operation(summary = "解雇服务员")
    public TableResult<String> deleteServer(@RequestBody WantDeleteDTO dto) {
        serverService.delete(dto.getWantDelete());
        return TableResult.success(MessageConstant.SERVER_DELETE_SUCCESS);
    }
}
