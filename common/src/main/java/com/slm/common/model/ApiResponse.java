package com.slm.common.model;

import com.slm.common.enums.ResultStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 统一返回对象
 *
 * @param <T>
 */
@Getter
@Schema(description = "统一返回对象")
public class ApiResponse<T> {

    @Schema(description = "状态码")
    private final int code;
    @Schema(description = "状态信息")
    private final String message;
    @Schema(description = "数据")
    private final T data;

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 调用成功（无数据返回）
     */
    public static ApiResponse<Void> success() {
        ResultStatus success = ResultStatus.SUCCESS;
        return new ApiResponse<>(success.getCode(), success.getMessage(), null);
    }

    /**
     * 调用成功（数据返回）
     */
    public static <T> ApiResponse<T> success(T data) {
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