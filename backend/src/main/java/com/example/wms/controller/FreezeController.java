package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.FreezeService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/freeze")
public class FreezeController {
    private final FreezeService freezeService;
    public FreezeController(FreezeService freezeService) { this.freezeService = freezeService; }

    @PostMapping("/create")
    public ApiResult<?> freeze(@RequestBody Map<String, Object> body) { return ApiResult.success(freezeService.freeze(body)); }

    @GetMapping("/list")
    public ApiResult<?> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) String status) {
        return ApiResult.success(freezeService.listFreezeRecords(keyword, status));
    }

    @GetMapping("/detail/{id}")
    public ApiResult<?> detail(@PathVariable Long id) { return ApiResult.success(freezeService.detail(id)); }

    @PostMapping("/unfreeze/{freezeId}")
    public ApiResult<?> unfreeze(@PathVariable Long freezeId, @RequestParam BigDecimal qty,
                                  @RequestParam String reason, @RequestParam(defaultValue = "admin") String operator) {
        return ApiResult.success(freezeService.unfreeze(freezeId, qty, reason, operator));
    }

    @GetMapping("/repack/list")
    public ApiResult<?> listRepack(@RequestParam(required = false) String keyword) {
        return ApiResult.success(freezeService.listRepackRecords(keyword));
    }

    @PostMapping("/repack")
    public ApiResult<?> repack(@RequestBody Map<String, Object> body) {
        return ApiResult.success(freezeService.repack(body));
    }
}
