package com.slm.barbershop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BizException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    public BizException(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
