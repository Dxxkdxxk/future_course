package com.lzlz.springboot.security.exception;

import com.lzlz.springboot.security.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * (!!!)
     * (!!!) 这就是你需要的“翻译器” (!!!)
     * (!!!)
     * 当任何 Controller 抛出 ResourceNotFoundException 时，此方法将被调用。
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // 1. 告诉 Spring 返回 404 状态码
    public
    ResponseEntity<ApiResponse<Object>> handleResourceNotFound(ResourceNotFoundException e) {

        // 2. 使用你的 ApiResponse 格式来构建错误消息
        // 这样前端就能收到你期望的 { "code": 404, "msg": "...", "data": null }
        ApiResponse<Object> errorResponse = ApiResponse.error(404, e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // (你还可以在这里添加其他异常处理器，例如处理 CustomGraphException)

    /**
     * 捕获你之前定义的 "CustomGraphException"
     */
    @ExceptionHandler(CustomGraphException.class)
    public
    ResponseEntity<ApiResponse<Object>> handleCustomGraphException(CustomGraphException e) {

        // 使用 CustomGraphException 中定义的 code 和 message
        ApiResponse<Object> errorResponse = ApiResponse.error(e.getCode(), e.getMessage());

        // 自定义异常通常返回 400 (Bad Request) 或 500 (Server Error)
        // 假设你的 30xxx 错误码是客户端输入错误，用 400
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * 捕获所有其他未处理的异常 (最后的保障)
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500 错误
    public
    ResponseEntity<ApiResponse<Object>> handleGenericException(Exception e) {
        // 打印堆栈信息，以便调试
        e.printStackTrace();

        ApiResponse<Object> errorResponse = ApiResponse.error(500, "服务器内部错误: " + e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}