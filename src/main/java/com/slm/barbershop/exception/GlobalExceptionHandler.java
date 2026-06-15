package com.slm.barbershop.exception;

import com.slm.barbershop.enums.ResultStatus;
import com.slm.barbershop.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Set;

/**
 * 统一异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * GET 请求参数校验异常
     */
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handler(BindException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldErrors().get(0);
        return ApiResponse.failure(ResultStatus.REJECT, fieldError.getDefaultMessage());
    }

    /**
     * Body 参数校验异常
     */
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handler(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldErrors().get(0);
        return ApiResponse.failure(ResultStatus.REJECT, fieldError.getDefaultMessage());
    }

    /**
     * {@code @Validated} 注解触发的校验
     */
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handler(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        return ApiResponse.failure(ResultStatus.REJECT, constraintViolations.iterator().next().getMessage());
    }

    /**
     * 业务异常，决定状态码方案
     *
     * @param e 业务异常
     * @return 响应
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handler(BizException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.failure(ResultStatus.REJECT, e.getMessage()));
    }

    /**
     * 上传文件超过最大限制
     */
    @ResponseStatus(code = HttpStatus.PAYLOAD_TOO_LARGE)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResponse<Void> handler(MaxUploadSizeExceededException e) {
        log.warn("文件大小超过限制: {}", e.getMessage());
        return ApiResponse.failure(ResultStatus.REJECT, "文件大小超过限制(20MB)");
    }

    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handler(Exception e) {
        log.error(e.getMessage(), e);
        return ApiResponse.failure(ResultStatus.ERROR, "服务器内部错误");
    }

}