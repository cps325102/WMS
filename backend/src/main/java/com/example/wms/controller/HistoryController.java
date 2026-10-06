package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.HistoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryService historyService;
    public HistoryController(HistoryService historyService) { this.historyService = historyService; }

    @GetMapping("/logs")
    public ApiResult<?> logs(@RequestParam(required = false) String businessType,
                              @RequestParam(required = false) String keyword,
                              @RequestParam(required = false) String startTime,
                              @RequestParam(required = false) String endTime,
                              @RequestParam(required = false) String operator) {
        return ApiResult.success(historyService.listLogs(businessType, keyword, startTime, endTime, operator));
    }

    @GetMapping("/trace/batch/{batchNo}")
    public ApiResult<?> traceByBatch(@PathVariable String batchNo) {
        return ApiResult.success(historyService.traceByBatch(batchNo));
    }

    @GetMapping("/trace/material/{materialCode}")
    public ApiResult<?> traceByMaterial(@PathVariable String materialCode) {
        return ApiResult.success(historyService.traceByMaterial(materialCode));
    }

    @GetMapping("/export")
    public ApiResult<?> export(@RequestParam(required = false) String businessType,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String startTime,
                                @RequestParam(required = false) String endTime,
                                @RequestParam(required = false) String operator) {
        return ApiResult.success(historyService.exportCsv(businessType, keyword, startTime, endTime, operator));
    }
}
