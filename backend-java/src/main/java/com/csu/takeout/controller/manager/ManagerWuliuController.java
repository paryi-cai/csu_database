package com.csu.takeout.controller.manager;

import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.ManagerOrderService;
import com.csu.takeout.vo.WuliuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端 —— 物流管理
 */
@RestController
@RequestMapping("/api/manager/wuliu")
@Tag(name = "管理端-物流管理")
@Slf4j
@RequiredArgsConstructor
public class ManagerWuliuController {

    private final ManagerOrderService managerOrderService;

    /**
     * 按是否结束查询物流记录
     *
     * <p>接口契约：查询参数名为 {@code id}；{@code id=0} 返回未结束（ended=0）的记录，
     * <b>其余任何取值（含缺省）都返回已结束（ended=1）的记录</b>。
     * 前端两个页面分别传 {@code ?id=0} 与 {@code ?id=1}。</p>
     */
    @GetMapping
    @Operation(summary = "查询物流记录", description = "id=0 未结束，其它值 已结束")
    public TableResult<WuliuVO> listWuliu(@RequestParam(value = "id", required = false) String id) {
        Integer ended = "0".equals(id) ? 0 : 1;
        List<WuliuVO> list = managerOrderService.listWuliu(ended);
        return TableResult.success(list);
    }
}
