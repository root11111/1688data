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
        if (args.length > 0 && args[0].equals("--crawl")) {
            // 运行爬虫模式
            runCrawler();
        } else {
            // 正常运行Spring Boot应用
            SpringApplication.run(Demo1Application.class, args);
        }
    }

    private static void runCrawler() {
        System.out.println("=== 1688爬虫模式 ===");
        
        // 启动Spring Boot应用
        ConfigurableApplicationContext context = SpringApplication.run(Demo1Application.class, new String[]{});
        
        try {
            // 获取服务
            AlibabaCrawlerService crawlerService = context.getBean(AlibabaCrawlerService.class);
            ExcelExportService excelExportService = context.getBean(ExcelExportService.class);
            
            System.out.println("开始爬取数据...");
            
            // 测试URL
            String testUrl = "https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AB%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AA%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true";
            
            // 爬取1页数据
            List<ManufacturerInfo> data = crawlerService.crawlManufacturerInfo(testUrl, 1);
            
            System.out.println("爬取完成，获取到 " + data.size() + " 条数据");
            
            if (!data.isEmpty()) {
                // 导出Excel
                boolean success = excelExportService.exportToDefaultPath(data);
                if (success) {
                    String fileName = excelExportService.generateDefaultFileName();
                    System.out.println("Excel导出成功: exports/" + fileName);
                } else {
                    System.out.println("Excel导出失败");
                }
                
                // 显示前3条数据
                System.out.println("\n数据预览:");
                for (int i = 0; i < Math.min(3, data.size()); i++) {
                    ManufacturerInfo info = data.get(i);
                    System.out.println((i+1) + ". " + info.getCompanyName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            context.close();
        }
    }
}
