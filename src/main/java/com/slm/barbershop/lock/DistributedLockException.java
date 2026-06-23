package com.slm.barbershop.lock;

import lombok.Getter;

@Getter
public class DistributedLockException extends RuntimeException {

    public DistributedLockException(String message) {
        super(message);
    }

}
