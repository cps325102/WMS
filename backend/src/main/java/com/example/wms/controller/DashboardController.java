package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping("/summary")
    public ApiResult<?> summary() { return ApiResult.success(dashboardService.getSummary()); }

    @GetMapping("/inbound-trend")
    public ApiResult<?> inboundTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResult.success(dashboardService.getInboundTrend(days));
    }

    @GetMapping("/outbound-trend")
    public ApiResult<?> outboundTrend(@RequestParam(defaultValue = "7") int days) {
        return ApiResult.success(dashboardService.getOutboundTrend(days));
    }

    @GetMapping("/slow-moving")
    public ApiResult<?> slowMoving(@RequestParam(defaultValue = "10") int n) {
        return ApiResult.success(dashboardService.getSlowMoving(n));
    }

    @GetMapping("/heatmap")
    public ApiResult<?> heatmap() { return ApiResult.success(dashboardService.getLocationHeatmap()); }

    @GetMapping("/turnover")
    public ApiResult<?> turnover() { return ApiResult.success(dashboardService.calcTurnover()); }

    @GetMapping("/completion-rate")
    public ApiResult<?> completionRate() { return ApiResult.success(dashboardService.calcCompletionRate()); }

    @GetMapping("/pending-tasks")
    public ApiResult<?> pendingTasks() { return ApiResult.success(dashboardService.getPendingTasks()); }
}
