package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.InboundOrderService;
import com.example.wms.service.KanbanService;
import com.example.wms.service.ReceiveService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inbound")
public class InboundController {
    private final InboundOrderService orderService;
    private final KanbanService kanbanService;
    private final ReceiveService receiveService;

    public InboundController(InboundOrderService orderService, KanbanService kanbanService, ReceiveService receiveService) {
        this.orderService = orderService;
        this.kanbanService = kanbanService;
        this.receiveService = receiveService;
    }

    // 入库单相关
    @PostMapping("/order/create")
    public ApiResult<?> createOrder(@RequestBody Map<String, Object> body) {
        return ApiResult.success(orderService.createOrder(body));
    }

    @PutMapping("/order/update/{id}")
    public ApiResult<?> updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResult.success(orderService.updateOrder(id, body));
    }

    @GetMapping("/order/list")
    public ApiResult<?> listOrders(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String status) {
        return ApiResult.success(orderService.listOrders(keyword, status));
    }

    @GetMapping("/order/detail/{id}")
    public ApiResult<?> detailOrder(@PathVariable Long id) {
        return ApiResult.success(orderService.detailOrder(id));
    }

    @DeleteMapping("/order/delete/{id}")
    public ApiResult<?> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ApiResult.success("删除成功", true);
    }

    @GetMapping("/order/print/{id}")
    public ApiResult<?> printOrder(@PathVariable Long id) {
        return ApiResult.success(orderService.detailOrder(id));
    }

    // 看板相关
    @PostMapping("/kanban/generate/{orderId}")
    public ApiResult<?> generateKanban(@PathVariable Long orderId) {
        return ApiResult.success(kanbanService.generateKanban(orderId));
    }

    @GetMapping("/kanban/list")
    public ApiResult<?> listKanbans(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String status) {
        return ApiResult.success(kanbanService.listKanbans(keyword, status));
    }

    @GetMapping("/kanban/print/{id}")
    public ApiResult<?> printKanban(@PathVariable Long id) {
        return ApiResult.success(kanbanService.printKanban(id));
    }

    @GetMapping("/kanban/scan/{kanbanCode}")
    public ApiResult<?> scanKanban(@PathVariable String kanbanCode) {
        return ApiResult.success(kanbanService.scanKanban(kanbanCode));
    }

    @GetMapping("/kanban/trace/{kanbanCode}")
    public ApiResult<?> kanbanTrace(@PathVariable String kanbanCode) {
        return ApiResult.success(kanbanService.kanbanTrace(kanbanCode));
    }

    // 收货
    @PostMapping("/receive")
    public ApiResult<?> receive(@RequestBody Map<String, Object> body) {
        return ApiResult.success(receiveService.receive(body));
    }

    // 反审核
    @PostMapping("/order/reverse-audit/{id}")
    public ApiResult<?> reverseAudit(@PathVariable Long id, @RequestParam String reason,
                                      @RequestParam(defaultValue = "admin") String operator) {
        return ApiResult.success(orderService.reverseAudit(id, reason, operator));
    }

    // 入库类型
    @GetMapping("/types")
    public ApiResult<?> inboundTypes() {
        return ApiResult.success(java.util.List.of("采购入库", "退货入库", "生产入库", "调拨入库", "其他入库"));
    }
}