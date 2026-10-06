package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.FifoConfigService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/fifo")
public class FifoConfigController {
    private final FifoConfigService fifoConfigService;
    public FifoConfigController(FifoConfigService fifoConfigService) { this.fifoConfigService = fifoConfigService; }

    @GetMapping("/config")
    public ApiResult<?> getActiveConfig() { return ApiResult.success(fifoConfigService.getActiveConfig()); }

    @GetMapping("/list")
    public ApiResult<?> list() { return ApiResult.success(fifoConfigService.listAll()); }

    @PostMapping("/switch/{mode}")
    public ApiResult<?> switchMode(@PathVariable String mode) { return ApiResult.success(fifoConfigService.switchMode(mode)); }

    @PutMapping("/update/{id}")
    public ApiResult<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResult.success(fifoConfigService.updateConfig(id, body));
    }
}
