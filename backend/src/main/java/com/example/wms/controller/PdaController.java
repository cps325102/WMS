package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.PdaService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/pda")
public class PdaController {
    private final PdaService pdaService;
    public PdaController(PdaService pdaService) { this.pdaService = pdaService; }

    @PostMapping("/login")
    public ApiResult<?> login(@RequestParam String username, @RequestParam String password) {
        return ApiResult.success(pdaService.login(username, password));
    }

    @GetMapping("/scan/{kanbanCode}")
    public ApiResult<?> scan(@PathVariable String kanbanCode) { return ApiResult.success(pdaService.scanKanban(kanbanCode)); }

    @GetMapping("/inventory")
    public ApiResult<?> inventory(@RequestParam(required = false) String keyword) {
        return ApiResult.success(pdaService.queryInventory(keyword));
    }

    @PostMapping("/receive")
    public ApiResult<?> receive(@RequestParam String kanbanCode, @RequestParam BigDecimal qty,
                                 @RequestParam(defaultValue = "admin") String operator,
                                 @RequestParam(required = false) String batchNo) {
        return ApiResult.success(pdaService.receive(kanbanCode, qty, operator, batchNo));
    }

    @GetMapping("/pick-tasks")
    public ApiResult<?> pickTasks(@RequestParam(defaultValue = "pda-001") String deviceId) {
        return ApiResult.success(pdaService.getPickTasks(deviceId));
    }

    @GetMapping("/verify-location")
    public ApiResult<?> verifyLocation(@RequestParam String locationCode, @RequestParam String materialCode) {
        return ApiResult.success(pdaService.verifyLocation(locationCode, materialCode));
    }

    @PostMapping("/check")
    public ApiResult<?> submitCheck(@RequestParam Long inventoryId, @RequestParam BigDecimal qty,
                                     @RequestParam(defaultValue = "admin") String operator,
                                     @RequestParam(defaultValue = "PDA盘点") String remark) {
        return ApiResult.success(pdaService.submitCheck(inventoryId, qty, operator, remark));
    }

    @PostMapping("/cache")
    public ApiResult<?> cacheOperation(@RequestParam String deviceId, @RequestParam String opType,
                                        @RequestBody Map<String, Object> data) {
        return ApiResult.success(pdaService.cacheOperation(deviceId, opType, data));
    }

    @GetMapping("/cache/{deviceId}")
    public ApiResult<?> getCachedOps(@PathVariable String deviceId) {
        return ApiResult.success(pdaService.getCachedOps(deviceId));
    }

    @PostMapping("/sync/{deviceId}")
    public ApiResult<?> sync(@PathVariable String deviceId) {
        return ApiResult.success(pdaService.syncOfflineData(deviceId));
    }
}
