package com.csu.takeout.controller.user;

import com.csu.takeout.result.TableResult;
import com.csu.takeout.service.ShopService;
import com.csu.takeout.vo.ShopVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户端 —— 店铺浏览
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户端-店铺")
@Slf4j
@RequiredArgsConstructor
public class UserShopController {

    private final ShopService shopService;

    /**
     * 获取店铺列表
     *
     * <p>接口契约：{@code {"status": 200, "tabledata": [{"shop_name": "..", "price": 15, "sale": 100}]}}。
     * 该接口在 既有接口中<b>不校验</b> token，此处保持一致。</p>
     */
    @GetMapping("/shop")
    @Operation(summary = "获取店铺列表")
    public TableResult<ShopVO> listShop() {
        List<ShopVO> shops = shopService.listAll();
        return TableResult.success(shops);
    }
}
