package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.InventoryMonitorService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class InventoryMonitorController {
    private final InventoryMonitorService monitorService;
    public InventoryMonitorController(InventoryMonitorService monitorService) { this.monitorService = monitorService; }

    @GetMapping("/overview")
    public ApiResult<?> overview() { return ApiResult.success(monitorService.getOverview()); }

    @GetMapping("/location-summary")
    public ApiResult<?> locationSummary() { return ApiResult.success(monitorService.getLocationSummary()); }

    @GetMapping("/alerts/check")
    public ApiResult<?> checkAlerts() { return ApiResult.success(monitorService.checkAlerts()); }

    @GetMapping("/alerts/list")
    public ApiResult<?> listAlerts(@RequestParam(required = false) String type,
                                    @RequestParam(required = false) String status) {
        return ApiResult.success(monitorService.listAlertLogs(type, status));
    }

    @PostMapping("/alerts/handle/{id}")
    public ApiResult<?> handleAlert(@PathVariable Long id, @RequestParam String handler,
                                     @RequestParam String action, @RequestParam(defaultValue = "") String remark) {
        return ApiResult.success(monitorService.handleAlert(id, handler, action, remark));
    }

    @GetMapping("/alerts/configs")
    public ApiResult<?> listAlertConfigs() {
        return ApiResult.success(monitorService.listAlertConfigs());
    }

    @PostMapping("/alerts/configs")
    public ApiResult<?> saveAlertConfig(@RequestBody Map<String, Object> body) {
        return ApiResult.success(monitorService.saveAlertConfig(body));
    }

    @DeleteMapping("/alerts/configs/{id}")
    public ApiResult<?> deleteAlertConfig(@PathVariable Long id) {
        monitorService.deleteAlertConfig(id);
        return ApiResult.success(true);
    }
}
