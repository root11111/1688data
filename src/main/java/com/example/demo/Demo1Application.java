package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Demo1Application {

    public static void main(String[] args) {
        // 正常启动Spring Boot应用，不自动执行爬虫任务
        ConfigurableApplicationContext context = SpringApplication.run(Demo1Application.class, args);
        
        // 添加应用关闭时的清理逻辑
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🔄 应用正在关闭，清理资源...");
            try {
                // 获取CrawlTaskService并关闭
                if (context.containsBean("crawlTaskService")) {
                    context.getBean("crawlTaskService", com.example.demo.service.CrawlTaskService.class).shutdown();
                }
                System.out.println("✅ 资源清理完成");
            } catch (Exception e) {
                System.err.println("❌ 资源清理失败: " + e.getMessage());
            }
        }));
        
        System.out.println("=== 1688爬虫管理系统已启动 ===");
        System.out.println("请访问 http://localhost:8080 来管理爬虫任务");
        System.out.println("✅ 自动重启调度器已启动，每10分钟检查一次失败任务");
    }

}
