package com.mqy.olearning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 在线学习平台启动类
 * 注意：本类所在包 com.mqy.olearning 是根包，所有业务类必须放在其子包下
 */
@SpringBootApplication
@MapperScan("com.mqy.olearning.mapper")
public class OlearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(OlearningApplication.class, args);
    }
}
