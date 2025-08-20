package com.example.demo;

import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.service.AlibabaCrawlerService;
import com.example.demo.service.ExcelExportService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication
public class Demo1Application {

    public static void main(String[] args) {
        // 检查是否有爬虫参数
        if (args.length > 0 && args[0].equals("--resume")) {
            // 断点续传模式
            runResumeCrawler();
        }

    }


    private static void runResumeCrawler() {
        System.out.println("=== 1688爬虫断点续传模式 ===");

        // 启动Spring Boot应用
        ConfigurableApplicationContext context = SpringApplication.run(Demo1Application.class, new String[]{});

        try {
            // 获取服务
            AlibabaCrawlerService crawlerService = context.getBean(AlibabaCrawlerService.class);
            ExcelExportService excelExportService = context.getBean(ExcelExportService.class);

            System.out.println("开始断点续传...");

            // 继续未完成的爬取任务
            List<ManufacturerInfo> data = crawlerService.resumeIncompleteTasks();

            System.out.println("断点续传完成，获取到 " + data.size() + " 条数据");

            if (!data.isEmpty()) {
                // 显示前3条数据
                System.out.println("\n数据预览:");
                for (int i = 0; i < Math.min(3, data.size()); i++) {
                    ManufacturerInfo info = data.get(i);
                    System.out.println((i + 1) + ". " + info.getCompanyName());
                }
            } else {
                System.out.println("没有新的数据需要处理");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            context.close();
        }
    }


}
