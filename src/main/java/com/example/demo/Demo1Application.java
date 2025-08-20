package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Demo1Application {

    public static void main(String[] args) {
        // 正常启动Spring Boot应用，不自动执行爬虫任务
        SpringApplication.run(Demo1Application.class, args);
        System.out.println("=== 1688爬虫管理系统已启动 ===");
        System.out.println("请访问 http://localhost:8080 来管理爬虫任务");
    }

}
