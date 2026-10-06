package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/list")
    public ApiResult<?> list(@RequestParam(required = false) String keyword) {
        return ApiResult.success(inventoryService.listInventory(keyword));
    }

    @GetMapping("/trace")
    public ApiResult<?> trace(@RequestParam(required = false) String keyword) {
        return ApiResult.success(inventoryService.trace(keyword));
    }
}