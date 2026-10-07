package com.example.wms.config;

import com.example.wms.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一把异常转成前端约定的 {code, message, data} 结构。
 *
 * 原本只有 AuthController 自己 try/catch，其余接口遇到业务异常会直接返回
 * Spring 默认的 500 错误页，前端拿不到 message。这里统一收口。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务校验失败：Service 抛的 RuntimeException 都是这类，属于预期内的提示。 */
    @ExceptionHandler(RuntimeException.class)
    public ApiResult<Void> handleBusiness(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResult.fail(e.getMessage() == null ? "操作失败" : e.getMessage());
    }

    /** 参数解析失败，通常是前端传来的 JSON 结构不对。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleBadBody(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return ApiResult.fail("请求参数格式错误");
    }

    /** 数据库约束拦截：外键冲突、唯一键重复、CHECK 不通过等。 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResult<Void> handleConstraint(DataIntegrityViolationException e) {
        log.warn("数据约束冲突: {}", e.getMostSpecificCause().getMessage());
        return ApiResult.fail("数据约束冲突：" + e.getMostSpecificCause().getMessage());
    }

    /** 兜底：未知异常只回笼统提示，堆栈记到服务端日志，不外泄实现细节。 */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleUnexpected(Exception e) {
        log.error("未预期异常", e);
        return ApiResult.fail("服务器内部错误，请查看后端日志");
    }
}
