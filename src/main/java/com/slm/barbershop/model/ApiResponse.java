package com.slm.barbershop.model;

import com.slm.barbershop.enums.ResultStatus;
import lombok.Getter;

/**
 * 统一返回对象
 *
 * @param <T>
 */
@Getter
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 调用成功（无数据返回）
     */
    public static ApiResponse<Void> ok() {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getMessage(), null);
    }

    /**
     * 调用成功（数据返回）
     */
    public static <T> ApiResponse<T> ok(T data) {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getMessage(), data);
    }

    /**
     * 调用失败
     */
    public static ApiResponse<Void> failure(ResultStatus status) {
        return new ApiResponse<>(status.getCode(), status.getMessage(), null);
    }

    /**
     * 调用失败（附带详细描述）
     */
    public static ApiResponse<Void> failure(ResultStatus status, String message) {
        return new ApiResponse<>(status.getCode(), message, null);
    }

}