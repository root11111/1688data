package com.example.demo.controller;

import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.service.AlibabaCrawlerService;
import com.example.demo.service.ExcelExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@CrossOrigin(origins = "*")
public class CrawlerController {

    private final AlibabaCrawlerService crawlerService;
    private final ExcelExportService excelExportService;

    public CrawlerController(AlibabaCrawlerService crawlerService, ExcelExportService excelExportService) {
        this.crawlerService = crawlerService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/1688")
    public ResponseEntity<Map<String, Object>> crawl1688Manufacturers(
            @RequestParam String url,
            @RequestParam(defaultValue = "3") int pages,
            @RequestParam(defaultValue = "false") boolean exportToExcel) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("开始爬取1688供应商信息...");
            System.out.println("目标URL: " + url);
            System.out.println("爬取页数: " + pages);
            System.out.println("导出Excel: " + exportToExcel);
            
            List<ManufacturerInfo> manufacturers = crawlerService.crawlManufacturerInfo(url, pages);
            
            // 如果需要导出Excel
            String excelFilePath = null;
            if (exportToExcel && !manufacturers.isEmpty()) {
                boolean exportSuccess = excelExportService.exportToDefaultPath(manufacturers);
                if (exportSuccess) {
                    excelFilePath = "exports/" + excelExportService.generateDefaultFileName();
                }
            }
            
            response.put("success", true);
            response.put("message", "爬取成功");
            response.put("data", manufacturers);
            response.put("total", manufacturers.size());
            response.put("excelFilePath", excelFilePath);
            
            System.out.println("爬取完成，共获取 " + manufacturers.size() + " 条供应商信息");
            if (excelFilePath != null) {
                System.out.println("Excel文件已导出到: " + excelFilePath);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("爬取过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "爬取失败: " + e.getMessage());
            response.put("data", null);
            response.put("total", 0);
            response.put("excelFilePath", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/1688/test")
    public ResponseEntity<Map<String, Object>> testCrawl1688(
            @RequestParam(defaultValue = "false") boolean exportToExcel) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 使用您提供的URL进行测试
            String testUrl = "https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AB%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AB%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true";
            
            System.out.println("开始测试爬取1688供应商信息...");
            System.out.println("导出Excel: " + exportToExcel);
            
            List<ManufacturerInfo> manufacturers = crawlerService.crawlManufacturerInfo(testUrl, 1);
            
            // 如果需要导出Excel
            String excelFilePath = null;
            if (exportToExcel && !manufacturers.isEmpty()) {
                boolean exportSuccess = excelExportService.exportToDefaultPath(manufacturers);
                if (exportSuccess) {
                    excelFilePath = "exports/" + excelExportService.generateDefaultFileName();
                }
            }
            
            response.put("success", true);
            response.put("message", "测试爬取成功");
            response.put("data", manufacturers);
            response.put("total", manufacturers.size());
            response.put("excelFilePath", excelFilePath);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("测试爬取过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "测试爬取失败: " + e.getMessage());
            response.put("data", null);
            response.put("total", 0);
            response.put("excelFilePath", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> exportToExcel(@RequestBody List<ManufacturerInfo> manufacturers) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (manufacturers == null || manufacturers.isEmpty()) {
                response.put("success", false);
                response.put("message", "没有数据可导出");
                response.put("excelFilePath", null);
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean exportSuccess = excelExportService.exportToDefaultPath(manufacturers);
            
            if (exportSuccess) {
                String excelFilePath = "exports/" + excelExportService.generateDefaultFileName();
                response.put("success", true);
                response.put("message", "Excel导出成功");
                response.put("excelFilePath", excelFilePath);
                response.put("total", manufacturers.size());
            } else {
                response.put("success", false);
                response.put("message", "Excel导出失败");
                response.put("excelFilePath", null);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("导出Excel过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "导出失败: " + e.getMessage());
            response.put("excelFilePath", null);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "爬虫服务正常运行");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}