package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.service.BasicDataService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/basic")
public class BasicController {
    private final BasicDataService basicDataService;

    public BasicController(BasicDataService basicDataService) {
        this.basicDataService = basicDataService;
    }

    @GetMapping("/{type}/list")
    public ApiResult<?> list(@PathVariable String type, @RequestParam(required = false) String keyword) {
        return switch (type) {
            case "material" -> ApiResult.success(basicDataService.listMaterials(keyword));
            case "supplier" -> ApiResult.success(basicDataService.listSuppliers(keyword));
            case "customer" -> ApiResult.success(basicDataService.listCustomers(keyword));
            case "warehouse" -> ApiResult.success(basicDataService.listWarehouses(keyword));
            case "location" -> ApiResult.success(basicDataService.listLocations(keyword));
            default -> ApiResult.fail("未知类型");
        };
    }

    @PostMapping("/{type}/save")
    public ApiResult<?> save(@PathVariable String type, @RequestBody Map<String, Object> body) {
        return switch (type) {
            case "material" -> ApiResult.success(basicDataService.saveMaterial(body));
            case "supplier" -> ApiResult.success(basicDataService.saveSupplier(body));
            case "customer" -> ApiResult.success(basicDataService.saveCustomer(body));
            case "warehouse" -> ApiResult.success(basicDataService.saveWarehouse(body));
            case "location" -> ApiResult.success(basicDataService.saveLocation(body));
            default -> ApiResult.fail("未知类型");
        };
    }

    @DeleteMapping("/{type}/delete/{id}")
    public ApiResult<?> delete(@PathVariable String type, @PathVariable Long id) {
        switch (type) {
            case "material" -> basicDataService.deleteMaterial(id);
            case "supplier" -> basicDataService.deleteSupplier(id);
            case "customer" -> basicDataService.deleteCustomer(id);
            case "warehouse" -> basicDataService.deleteWarehouse(id);
            case "location" -> basicDataService.deleteLocation(id);
            default -> throw new RuntimeException("未知类型");
        }
        return ApiResult.success("删除成功", true);
    }
}
