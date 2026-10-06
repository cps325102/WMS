package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.MenuItem;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {
    @GetMapping("/list")
    public ApiResult<List<MenuItem>> list() {
        List<MenuItem> menus = new ArrayList<>();

        // 首页
        menus.add(new MenuItem("首页", "/home", "HomeFilled"));

        // 基础数据
        menus.add(new MenuItem("基础数据", "FolderOpened", Arrays.asList(
            new MenuItem("物料管理", "/basic/material", "Box"),
            new MenuItem("供应商管理", "/basic/supplier", "User"),
            new MenuItem("客户管理", "/basic/customer", "Avatar"),
            new MenuItem("仓库管理", "/basic/warehouse", "OfficeBuilding"),
            new MenuItem("库位管理", "/basic/location", "Location")
        )));

        // 入库管理
        menus.add(new MenuItem("入库管理", "Sell", Arrays.asList(
            new MenuItem("入库单管理", "/inbound/manage", "Tickets"),
            new MenuItem("看板生命周期", "/inbound/kanban/lifecycle", "Document")
        )));

        // 出库管理
        menus.add(new MenuItem("出库管理", "ShoppingCartFull", Arrays.asList(
            new MenuItem("出库单管理", "/outbound/orders", "ShoppingCart")
        )));

        // 库存管理
        menus.add(new MenuItem("库存管理", "Goods", Arrays.asList(
            new MenuItem("库存监控", "/inventory/monitor", "Odometer"),
            new MenuItem("封存转包", "/freeze/manage", "Lock"),
            new MenuItem("AI智能预警", "/inventory/alerts", "Warning"),
            new MenuItem("看板监控", "/inventory/manage", "TrendCharts"),
            new MenuItem("库存流水", "/history/logs", "List")
        )));

        // 看板可视化
        menus.add(new MenuItem("看板可视化", "/dashboard", "DataAnalysis"));

        return ApiResult.success(menus);
    }
}
