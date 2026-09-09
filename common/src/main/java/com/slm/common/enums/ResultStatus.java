package com.slm.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 结果状态码
 *
 * <p>业务 code 分段建议（便于管理和排查）：</p>
 *
 * <ul>
 * <li>0：成功</li>
 * <li>1xxx：通用错误（如参数错误、签名错误）</li>
 * <li>2xxx：用户/权限相关（如未登录、无权限、账号冻结）</li>
 * <li>3xxx：业务逻辑错误（如库存不足、订单已支付）</li>
 * <li>4xxx：第三方服务错误（如短信发送失败、支付渠道异常）</li>
 * <li>5xxx：服务端内部错误（数据库、缓存、文件系统）</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ResultStatus {

    SUCCESS(0, "调用成功", HttpStatus.OK),
    ERROR(-1, "未知异常", HttpStatus.INTERNAL_SERVER_ERROR),

    BAD_REQUEST(1000, "请求参数错误", HttpStatus.BAD_REQUEST),
    SOURCE_NOT_FOUND(1002, "资源不存在", HttpStatus.NOT_FOUND),
    FILE_SIZE_EXCEEDED(1003, "文件大小超出限制", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_UPLOAD_ERROR(1004, "上传文件异常", HttpStatus.BAD_REQUEST),
    SOURCE_CONFLICT(1005, "资源冲突", HttpStatus.CONFLICT),

    UNAUTHORIZED(2000, "未登录", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(2001, "无效TOKEN", HttpStatus.UNAUTHORIZED),
    PERMISSION_DENIED(2002, "权限不足", HttpStatus.FORBIDDEN),
    USER_OR_PASSWORD_WRONG(2003, "用户名或密码错误", HttpStatus.FORBIDDEN),
    INVALID_VERIFY_CODE(2004, "无效验证码", HttpStatus.FORBIDDEN),

    MINIO_INIT_ERROR(5000, "MinIO 初始化失败", HttpStatus.SERVICE_UNAVAILABLE),
    MINIO_OPERATION_ERROR(5001, "MinIO 操作失败", HttpStatus.SERVICE_UNAVAILABLE),
    EMAIL_SEND_ERROR(5002, "邮件发送失败", HttpStatus.SERVICE_UNAVAILABLE),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

}
