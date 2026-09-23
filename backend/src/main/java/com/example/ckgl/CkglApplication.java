package com.example.ckgl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.ckgl.mapper")
public class CkglApplication {

    public static void main(String[] args) {
        SpringApplication.run(CkglApplication.class, args);
    }
}
