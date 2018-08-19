package com.westlake.air.pecs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PecsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PecsPlatformApplication.class, args);
    }
}
