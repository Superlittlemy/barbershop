package com.slm.barbershop.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.slm.barbershop.enums.ResultStatus;
import com.slm.barbershop.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class ResponseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void setResponse(HttpServletResponse response, HttpStatus httpStatus, String message) {
        // 设置响应状态码
        response.setStatus(httpStatus.value());
        // 设置正确的编码和内容类型（解决中文乱码问题）
        response.setCharacterEncoding("UTF-8"); // 设置字符编码
        response.setContentType("application/json;charset=UTF-8"); // 设置完整的内容类型
        // 构建错误响应
        try {
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(ResultStatus.ERROR, message)));
        } catch (IOException ioException) {
            log.error("Failed to write error response", ioException);
        }
    }

}
