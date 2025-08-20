package com.example.demo;

import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.service.AlibabaCrawlerService;
import com.example.demo.service.ExcelExportService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication
public class SimpleCrawlerTest {

    public static void main(String[] args) {

        
        // 启动Spring Boot应用
        ConfigurableApplicationContext context = SpringApplication.run(SimpleCrawlerTest.class, args);
        System.out.println("=== 1688爬虫测试（增强版） ===");
        System.out.println("🔄 启动Spring Boot应用...");
        try {
            // 获取服务
            System.out.println("🔧 正在获取服务...");
            AlibabaCrawlerService crawlerService = context.getBean(AlibabaCrawlerService.class);
            ExcelExportService excelExportService = context.getBean(ExcelExportService.class);
            
            System.out.println("✅ 服务初始化完成");
            System.out.println("🚀 开始爬取数据...");
            
            // 测试URL
            String testUrl = "https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AB%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AA%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true";
            
            System.out.println("📋 目标URL: " + testUrl);
            System.out.println("📄 爬取页数: 1页");


            System.out.println("🔄 正在调用爬虫服务...");
            List<ManufacturerItem> data = crawlerService.crawlManufacturerInfo(testUrl, 50, null);
            
            System.out.println("✅ 爬取完成，获取到 " + data.size() + " 条数据");
            System.out.println("📊 数据详情检查:");
            System.out.println("   - 数据列表是否为null: " + (data == null));
            System.out.println("   - 数据列表是否为空: " + (data != null && data.isEmpty()));
            if (data != null && !data.isEmpty()) {
                System.out.println("   - 第一条数据公司名: " + data.get(0).getCompanyName());
                System.out.println("   - 第一条数据商品标题: " + data.get(0).getProductTitle());
            }
            
            // 强制尝试导出Excel，无论数据是否为空
            System.out.println("📊 开始导出Excel...");
            try {
                boolean success = excelExportService.exportToDefaultPath(data);
                System.out.println("📊 Excel导出结果: " + success);
                
                if (success) {
                    String fileName = excelExportService.generateDefaultFileName();
                    System.out.println("✅ Excel导出成功: exports/" + fileName);
                    System.out.println("📁 文件位置: " + System.getProperty("user.dir") + "/exports/" + fileName);
                    
                    // 验证文件是否真的存在
                    java.io.File file = new java.io.File("exports/" + fileName);
                    if (file.exists()) {
                        System.out.println("✅ 文件确实存在，大小: " + file.length() + " 字节");
                    } else {
                        System.out.println("❌ 文件不存在，导出可能失败");
                    }
                } else {
                    System.out.println("❌ Excel导出失败");
                }
            } catch (Exception e) {
                System.err.println("❌ Excel导出过程中出现错误: " + e.getMessage());
                System.err.println("🔍 Excel导出错误详情:");
                e.printStackTrace();
            }
            
            // 显示数据预览（如果有数据）
            if (data != null && !data.isEmpty()) {
                System.out.println("\n📋 数据预览:");
                for (int i = 0; i < Math.min(3, data.size()); i++) {
                    ManufacturerInfo info = data.get(i);
                    System.out.println((i+1) + ". " + info.getCompanyName());
                    System.out.println("   商品: " + info.getProductTitle());
                    System.out.println("   价格: " + info.getPrice());
                    System.out.println("   联系方式: " + info.getContactInfo());
                    System.out.println();
                }
                
                // 显示统计信息
                System.out.println("📈 统计信息:");
                System.out.println("   总数据条数: " + data.size());
                System.out.println("   有公司名称的数据: " + data.stream().filter(d -> d.getCompanyName() != null && !d.getCompanyName().isEmpty()).count());
                System.out.println("   有联系方式的数据: " + data.stream().filter(d -> d.getContactInfo() != null && !d.getContactInfo().isEmpty()).count());
                System.out.println("   有价格信息的数据: " + data.stream().filter(d -> d.getPrice() != null && !d.getPrice().isEmpty()).count());
            } else {
                System.out.println("⚠️  未获取到任何数据，可能的原因：");
                System.out.println("   1. 网络连接问题");
                System.out.println("   2. 验证码未正确处理");
                System.out.println("   3. 页面结构发生变化");
                System.out.println("   4. 目标网站反爬虫机制");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 爬取过程中出现错误: " + e.getMessage());
            System.err.println("🔍 错误详情:");
            e.printStackTrace();
            
            System.out.println("\n💡 故障排除建议:");
            System.out.println("   1. 检查网络连接");
            System.out.println("   2. 确认Chrome浏览器版本兼容性");
            System.out.println("   3. 检查目标网站是否可访问");
            System.out.println("   4. 尝试手动访问目标URL");
            System.out.println("   5. 查看详细错误日志");
            
        } finally {
            System.out.println("🔄 关闭Spring Boot应用...");
            try {
                context.close();
                System.out.println("✅ Spring Boot应用已关闭");
            } catch (Exception e) {
                System.err.println("❌ 关闭Spring Boot应用时出错: " + e.getMessage());
            }
            System.out.println("✅ 测试完成");
        }
    }
} 