package com.travelmate;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * TravelMate Spring Boot 启动类
 */
@SpringBootApplication
@EnableCaching
@MapperScan("com.travelmate.mapper")
public class TravelmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelmateApplication.class, args);
    }
}
