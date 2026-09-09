package com.slm.common.exception;

import com.slm.common.enums.ResultStatus;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Getter
public class BizException extends RuntimeException {

    private final ResultStatus status;
    private final String message;

    public BizException(ResultStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
