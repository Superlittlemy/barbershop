package com.slm.barbershop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;


@Aspect
@Component
public class ApiLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogAspect.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Pointcut("execution(public * com.slm.barbershop.controller..*.*(..))")
    public void apiLog() {}

    @Before("apiLog()")
    public void doBefore(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                logger.info("==================== Request Start ====================");
                logger.info("URL            : {}", request.getRequestURL().toString());
                logger.info("HTTP Method    : {}", request.getMethod());
                logger.info("Class Method   : {}.{}",
                        joinPoint.getSignature().getDeclaringTypeName(),
                        joinPoint.getSignature().getName());
                logger.info("IP             : {}", request.getRemoteAddr());
                Object[] safeArgs = Arrays.stream(joinPoint.getArgs())
                        .map(a -> a instanceof MultipartFile
                                ? "MultipartFile[" + ((MultipartFile) a).getOriginalFilename()
                                + ", " + ((MultipartFile) a).getSize() + "B]"
                                : a)
                        .toArray();
                logger.info("Request Args   : {}", Arrays.toString(safeArgs));
            }
        } catch (Exception e) {
            logger.error("记录请求日志失败", e);
        }
    }

    @AfterReturning(pointcut = "apiLog()", returning = "result")
    public void doAfterReturning(Object result) {
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            logger.info("Response Result: {}", resultJson);
            logger.info("==================== Request End ====================");
        } catch (Exception e) {
            logger.error("记录响应日志失败", e);
        }
    }

    @AfterThrowing(pointcut = "apiLog()", throwing = "e")
    public void doAfterThrowing(Throwable e) {
        // 走统一异常处理器
        // logger.error("接口异常: {}", e.getMessage(), e);
    }

}
