package com.csu.takeout.controller.manager;

import com.csu.takeout.constant.MessageConstant;
import com.csu.takeout.dto.DispatcherAddDTO;
import com.csu.takeout.dto.WantDeleteDTO;
import com.csu.takeout.entity.Dispatcher;
import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.DispatcherService;
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
 * 管理端 —— 配送员管理
 */
@RestController
@RequestMapping("/api/manager/dispatcher")
@Tag(name = "管理端-配送员管理")
@Slf4j
@RequiredArgsConstructor
public class ManagerDispatcherController {

    private final DispatcherService dispatcherService;

    /** 配送员列表（Flask 版不额外返回候选列表） */
    @GetMapping
    @Operation(summary = "查询配送员列表")
    public TableResult<Dispatcher> listDispatcher() {
        List<Dispatcher> dispatchers = dispatcherService.listAll();
        return TableResult.success(dispatchers);
    }

    /**
     * 新增配送员
     *
     * <p>响应：编号已存在 → {@code {"status":1000,"msg":"该编号已存在"}}；
     * 成功 → {@code {"status":200,"msg":"添加成功"}}</p>
     */
    @PostMapping
    @Operation(summary = "新增配送员")
    public TableResult<String> addDispatcher(@RequestBody DispatcherAddDTO dto) {
        boolean success = dispatcherService.add(
                dto.getDispatcherId(), dto.getDispatcherName(), dto.getDispatcherPhone());
        return success
                ? TableResult.success(MessageConstant.DISPATCHER_ADD_SUCCESS)
                : TableResult.error(MessageConstant.DISPATCHER_ALREADY_EXISTS);
    }

    /** 解雇配送员（请求体字段 {@code want_delete} 为配送员编号） */
    @DeleteMapping
    @Operation(summary = "解雇配送员")
    public TableResult<String> deleteDispatcher(@RequestBody WantDeleteDTO dto) {
        dispatcherService.delete(dto.getWantDelete());
        return TableResult.success(MessageConstant.DISPATCHER_DELETE_SUCCESS);
    }
}
