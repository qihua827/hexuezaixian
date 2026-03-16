package com.xj.hexuezaixian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.IOException;

@SpringBootApplication
@EnableAsync
public class HexuezaixianApplication {

    public static void main(String[] args) throws IOException, InterruptedException {

        SpringApplication.run(HexuezaixianApplication.class, args);

    }

}
