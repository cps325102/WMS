package com.example.wms.controller;

import com.example.wms.dto.ApiResult;
import com.example.wms.dto.LoginRequest;
import com.example.wms.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        if ("admin".equals(request.getUsername()) && "123456".equals(request.getPassword())) {
            return ApiResult.success("登录成功", new LoginResponse("mock-token-admin", "admin", "管理员"));
        }
        return ApiResult.fail("用户名或密码错误");
    }
}
