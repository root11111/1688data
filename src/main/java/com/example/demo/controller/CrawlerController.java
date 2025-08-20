package com.example.demo.controller;

import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.service.AlibabaCrawlerService;
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

    public CrawlerController(AlibabaCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @GetMapping("/1688")
    public ResponseEntity<Map<String, Object>> crawl1688Manufacturers(
            @RequestParam String url,
            @RequestParam(defaultValue = "3") int pages) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("开始爬取1688供应商信息...");
            System.out.println("目标URL: " + url);
            System.out.println("爬取页数: " + pages);
            
            List<ManufacturerInfo> manufacturers = crawlerService.crawlManufacturerInfo(url, pages);
            
            response.put("success", true);
            response.put("message", "爬取成功，数据已保存到数据库");
            response.put("data", manufacturers);
            response.put("total", manufacturers.size());
            
            System.out.println("爬取完成，共获取 " + manufacturers.size() + " 条供应商信息，已保存到数据库");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("爬取过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "爬取失败: " + e.getMessage());
            response.put("data", null);
            response.put("total", 0);
            
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/1688/test")
    public ResponseEntity<Map<String, Object>> testCrawl1688() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 使用您提供的URL进行测试
            String testUrl = "https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AB%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AB%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true";
            
            System.out.println("开始测试爬取1688供应商信息...");
            
            List<ManufacturerInfo> manufacturers = crawlerService.crawlManufacturerInfo(testUrl, 1);
            

            
            response.put("success", true);
            response.put("message", "测试爬取成功，数据已保存到数据库");
            response.put("data", manufacturers);
            response.put("total", manufacturers.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("测试爬取过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "测试爬取失败: " + e.getMessage());
            response.put("data", null);
            response.put("total", 0);
            
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