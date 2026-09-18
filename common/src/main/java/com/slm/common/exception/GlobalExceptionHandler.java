package com.slm.common.exception;

import com.slm.common.enums.ResultStatus;
//import com.slm.barbershop.lock.DistributedLockException;
import com.slm.common.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 统一异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * GET 请求参数校验异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handler(BindException e) {
        String msg = collectFieldErrors(e.getBindingResult());
        log.warn("参数绑定校验失败: {}", msg);
        return ResponseEntity
                .status(ResultStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.BAD_REQUEST, msg));
    }

    /**
     * Body 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handler(MethodArgumentNotValidException e) {
        String msg = collectFieldErrors(e.getBindingResult());
        log.warn("Body 参数校验失败: {}", msg);
        return ResponseEntity
                .status(ResultStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.BAD_REQUEST, msg));
    }

    /**
     * {@code @Validated} 注解触发的校验
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handler(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}", msg);
        return ResponseEntity
                .status(ResultStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.BAD_REQUEST, msg));
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handler(MissingServletRequestParameterException e) {
        String msg = String.format("缺少必填参数: %s (%s)", e.getParameterName(), e.getParameterType());
        log.warn(msg);
        return ResponseEntity
                .status(ResultStatus.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.BAD_REQUEST, msg));
    }

    /**
     * 请求方法不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handler(HttpRequestMethodNotSupportedException e) {
        String supported = e.getSupportedHttpMethods() == null ? "" :
                e.getSupportedHttpMethods().stream().map(Object::toString).collect(Collectors.joining(", "));
        String msg = String.format("请求方法 %s 不支持，支持的方法: %s", e.getMethod(), supported);
        log.warn(msg);
        return ResponseEntity
                .status(ResultStatus.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.METHOD_NOT_ALLOWED, msg));
    }

    /**
     * Content-Type 不支持
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handler(HttpMediaTypeNotSupportedException e) {
        String msg = String.format("不支持的媒体类型: %s", e.getContentType());
        log.warn(msg);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.failure(ResultStatus.BAD_REQUEST, msg));
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handler(BizException e) {
        log.warn("业务异常: [{}] {}", e.getStatus().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getStatus().getHttpStatus())
                .body(ApiResponse.failure(e.getStatus(), e.getMessage()));
    }

    /**
     * 上传文件超过最大限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handler(MaxUploadSizeExceededException e) {
        log.warn("文件大小超过限制: {}", e.getMessage());
        return ResponseEntity
                .status(ResultStatus.FILE_SIZE_EXCEEDED.getHttpStatus())
                .body(ApiResponse.failure(ResultStatus.FILE_SIZE_EXCEEDED, "文件大小超过限制(20MB)"));
    }

    /**
     * 未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handler(Exception e) {
        log.error("服务器内部异常: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ResultStatus.ERROR, "服务器内部错误"));
    }

    /**
     * 锁冲突
     */
//    @ResponseStatus(code = HttpStatus.SERVICE_UNAVAILABLE)
//    @ExceptionHandler(DistributedLockException.class)
//    public ApiResponse<Void> handler(DistributedLockException e) {
//        log.warn("锁冲突: {}", e.getMessage());
//        return ApiResponse.failure(ResultStatus.REJECT, e.getMessage());
//    }

    private String collectFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

}
