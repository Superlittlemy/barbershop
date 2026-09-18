package com.slm.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Aspect
@Component
public class ApiLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiLogAspect.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Pointcut("execution(public * com.slm..controller..*.*(..))")
    public void apiLog() {}

    @Around("apiLog()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable thrown = null;
        try {
            logRequest(pjp);
            result = pjp.proceed();
            return result;
        } catch (Throwable e) {
            thrown = e;
            throw e;
        } finally {
            logResponse(pjp, result, thrown, System.currentTimeMillis() - start);
        }
    }

    private void logRequest(ProceedingJoinPoint pjp) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            logger.info("==================== Request Start ====================");
            logger.info("URL            : {}", request.getRequestURL().toString());
            logger.info("HTTP Method    : {}", request.getMethod());
            logger.info("Class Method   : {}.{}",
                    pjp.getSignature().getDeclaringTypeName(),
                    pjp.getSignature().getName());
            logger.info("IP             : {}", request.getRemoteAddr());
            Object[] safeArgs = Arrays.stream(pjp.getArgs())
                    .map(a -> a instanceof MultipartFile
                            ? "MultipartFile[" + ((MultipartFile) a).getOriginalFilename()
                            + ", " + ((MultipartFile) a).getSize() + "B]"
                            : a)
                    .toArray();
            logger.info("Request Args   : {}", Arrays.toString(safeArgs));
        } catch (Exception e) {
            logger.error("记录请求日志失败", e);
        }
    }

    private void logResponse(ProceedingJoinPoint pjp, Object result, Throwable thrown, long costMs) {
        try {
            if (thrown == null) {
                String resultJson = safeToJson(result);
                logger.info("Response Result: {}", resultJson);
            } else {
                logger.warn("Response Error : {} - {}", thrown.getClass().getSimpleName(), thrown.getMessage());
            }
            logger.info("Cost           : {} ms", costMs);
            logger.info("==================== Request End ======================");
        } catch (Exception e) {
            logger.error("记录响应日志失败", e);
        }
    }

    private String safeToJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "<unserializable: " + value.getClass().getName() + ">";
        }
    }
}
