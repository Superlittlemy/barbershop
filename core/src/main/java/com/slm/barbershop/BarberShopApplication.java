package com.slm.barbershop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.slm" })
public class BarberShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarberShopApplication.class, args);
    }

}
