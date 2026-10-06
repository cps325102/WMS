package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.OutboundService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/outbound")
public class OutboundController {
    private final OutboundService outboundService;

    public OutboundController(OutboundService outboundService) {
        this.outboundService = outboundService;
    }

    @PostMapping("/order/create")
    public ApiResult<?> createOrder(@RequestBody Map<String, Object> body) {
        return ApiResult.success(outboundService.createOutboundOrder(body));
    }

    @PutMapping("/order/update/{id}")
    public ApiResult<?> updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResult.success(outboundService.updateOutboundOrder(id, body));
    }

    @GetMapping("/order/list")
    public ApiResult<?> listOrders(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status) {
        return ApiResult.success(outboundService.listOutboundOrders(keyword, status));
    }

    @GetMapping("/order/detail/{id}")
    public ApiResult<?> detailOrder(@PathVariable Long id) {
        return ApiResult.success(outboundService.detailOutboundOrder(id));
    }

    @DeleteMapping("/order/delete/{id}")
    public ApiResult<?> deleteOrder(@PathVariable Long id) {
        outboundService.deleteOutboundOrder(id);
        return ApiResult.success("删除成功", true);
    }

    // 执行出库（先进先出）
    @PostMapping("/order/execute/{id}")
    public ApiResult<?> executeOrder(@PathVariable Long id) {
        return ApiResult.success(outboundService.executeOutbound(id));
    }

    // 扫码出库（看板级别）
    @PostMapping("/ship")
    public ApiResult<?> shipOutbound(@RequestBody Map<String, Object> body) {
        return ApiResult.success(outboundService.shipOutbound(body));
    }

    @GetMapping("/inventory/available/{materialId}")
    public ApiResult<?> getAvailableInventory(@PathVariable Long materialId) {
        return ApiResult.success(outboundService.getAvailableInventoryForMaterial(materialId));
    }

    @GetMapping("/materials/with-stock")
    public ApiResult<?> getMaterialsWithStock() {
        return ApiResult.success(outboundService.getMaterialsWithStock());
    }

    // 反审核
    @PostMapping("/order/reverse-audit/{id}")
    public ApiResult<?> reverseAudit(@PathVariable Long id, @RequestParam String reason,
                                      @RequestParam(defaultValue = "admin") String operator) {
        return ApiResult.success(outboundService.reverseAudit(id, reason, operator));
    }

    // 拣货指引
    @GetMapping("/picking-tasks/{orderId}")
    public ApiResult<?> pickingTasks(@PathVariable Long orderId) {
        return ApiResult.success(outboundService.getPickingTasks(orderId));
    }

    // 出库类型
    @GetMapping("/types")
    public ApiResult<?> outboundTypes() {
        return ApiResult.success(java.util.List.of("销售出库", "领料出库", "调拨出库", "其他出库"));
    }
}