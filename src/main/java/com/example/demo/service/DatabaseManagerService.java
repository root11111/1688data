package com.example.demo.service;

import com.example.demo.entity.CrawlProgress;
import com.example.demo.repository.CrawlProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DatabaseManagerService {
    
    @Autowired
    private CrawlProgressRepository crawlProgressRepository;
    
    /**
     * 查看所有爬取进度
     */
    public List<CrawlProgress> getAllProgress() {
        return crawlProgressRepository.findAll();
    }
    
    /**
     * 根据URL查找进度
     */
    public Optional<CrawlProgress> findByUrl(String url) {
        return crawlProgressRepository.findByUrl(url);
    }
    
    /**
     * 修改爬取进度到指定页面和商品索引
     */
    @Transactional
    public boolean updateProgressToPage(String url, int targetPage, int targetItemIndex) {
        Optional<CrawlProgress> optional = crawlProgressRepository.findByUrl(url);
        if (optional.isPresent()) {
            CrawlProgress progress = optional.get();
            progress.setCurrentPage(targetPage);
            progress.setCurrentItemIndex(targetItemIndex);
            progress.setStatus("IN_PROGRESS");
            progress.setUpdatedTime(java.time.LocalDateTime.now());
            crawlProgressRepository.save(progress);
            
            System.out.println("✅ 数据库进度已更新:");
            System.out.println("   URL: " + progress.getUrl());
            System.out.println("   当前页: " + progress.getCurrentPage());
            System.out.println("   当前商品索引: " + progress.getCurrentItemIndex());
            System.out.println("   状态: " + progress.getStatus());
            return true;
        } else {
            System.out.println("❌ 未找到URL为 " + url + " 的进度记录");
            return false;
        }
    }
    
    /**
     * 重置爬取进度到第一页
     */
    @Transactional
    public boolean resetProgressToFirstPage(String url) {
        return updateProgressToPage(url, 1, 0);
    }
    
    /**
     * 设置爬取进度到第二页开始
     */
    @Transactional
    public boolean setProgressToSecondPage(String url) {
        return updateProgressToPage(url, 2, 0);
    }
    
    /**
     * 删除指定URL的进度记录
     */
    @Transactional
    public boolean deleteProgress(String url) {
        Optional<CrawlProgress> optional = crawlProgressRepository.findByUrl(url);
        if (optional.isPresent()) {
            crawlProgressRepository.delete(optional.get());
            System.out.println("✅ 已删除URL为 " + url + " 的进度记录");
            return true;
        } else {
            System.out.println("❌ 未找到URL为 " + url + " 的进度记录");
            return false;
        }
    }
    
    /**
     * 显示所有进度记录
     */
    public void displayAllProgress() {
        List<CrawlProgress> allProgress = getAllProgress();
        if (allProgress.isEmpty()) {
            System.out.println("📋 数据库中没有爬取进度记录");
            return;
        }
        
        System.out.println("📋 数据库中的爬取进度记录:");
        System.out.println("==========================================");
        for (CrawlProgress progress : allProgress) {
            System.out.println("ID: " + progress.getId());
            System.out.println("URL: " + progress.getUrl());
            System.out.println("当前页: " + progress.getCurrentPage());
            System.out.println("当前商品索引: " + progress.getCurrentItemIndex());
            System.out.println("总页数: " + progress.getTotalPages());
            System.out.println("状态: " + progress.getStatus());
            System.out.println("创建时间: " + progress.getCreatedTime());
            System.out.println("更新时间: " + progress.getUpdatedTime());
            System.out.println("------------------------------------------");
        }
    }
}
