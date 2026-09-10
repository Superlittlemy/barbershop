package com.slm.storage;

import com.slm.common.config.WildcardMapperScanRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(WildcardMapperScanRegistrar.class)
@SpringBootApplication(scanBasePackages = "com.slm")
public class StorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }

}
