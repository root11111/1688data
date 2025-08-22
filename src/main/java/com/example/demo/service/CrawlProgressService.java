package com.example.demo.service;

import com.example.demo.entity.CrawlProgress;
import com.example.demo.repository.CrawlProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CrawlProgressService {
    
    @Autowired
    private CrawlProgressRepository crawlProgressRepository;
    
    /**
     * 创建新的爬取进度记录
     */
    @Transactional
    public CrawlProgress createProgress(String url, Integer totalPages, Long taskId) {
        CrawlProgress progress = new CrawlProgress(url, totalPages);
        progress.setTaskId(taskId);
        return crawlProgressRepository.save(progress);
    }
    
    /**
     * 根据URL查找爬取进度
     */
    public Optional<CrawlProgress> findByUrl(String url) {
        return crawlProgressRepository.findByUrl(url);
    }
    
    /**
     * 更新爬取进度
     */
    @Transactional(rollbackFor = Exception.class)
    public CrawlProgress updateProgress(Long progressId, Integer currentPage, Integer currentItemIndex, String status) {
        try {
            System.out.println("🔄 CrawlProgressService.updateProgress 被调用:");
            System.out.println("   - 进度记录ID: " + progressId);
            System.out.println("   - 目标页码: " + currentPage);
            System.out.println("   - 目标项索引: " + currentItemIndex);
            System.out.println("   - 目标状态: " + status);
            
            // 🆕 添加参数验证
            if (progressId == null) {
                System.err.println("❌ 进度记录ID为null");
                return null;
            }
            if (currentPage == null) {
                System.err.println("❌ 目标页码为null");
                return null;
            }
            if (currentItemIndex == null) {
                System.err.println("❌ 目标项索引为null");
                return null;
            }
            if (status == null) {
                System.err.println("❌ 目标状态为null");
                return null;
            }
            
            Optional<CrawlProgress> optional = crawlProgressRepository.findById(progressId);
            if (optional.isPresent()) {
                CrawlProgress progress = optional.get();
                System.out.println("   - 找到进度记录: " + progress.getUrl());
                System.out.println("   - 当前页码: " + progress.getCurrentPage());
                System.out.println("   - 当前项索引: " + progress.getCurrentItemIndex());
                
                progress.setCurrentPage(currentPage);
                progress.setCurrentItemIndex(currentItemIndex);
                progress.setStatus(status);
                progress.setLastCrawledTime(LocalDateTime.now());
                progress.setUpdatedTime(LocalDateTime.now());
                
                System.out.println("   - 设置新值后:");
                System.out.println("     - 内存中页码: " + progress.getCurrentPage());
                System.out.println("     - 内存中项索引: " + progress.getCurrentItemIndex());
                System.out.println("     - 内存中状态: " + progress.getStatus());
                
                CrawlProgress savedProgress = crawlProgressRepository.save(progress);
                System.out.println("   - 保存后返回的对象:");
                System.out.println("     - 返回对象页码: " + savedProgress.getCurrentPage());
                System.out.println("     - 返回对象项索引: " + savedProgress.getCurrentItemIndex());
                System.out.println("     - 返回对象状态: " + savedProgress.getStatus());
                
                // 🆕 强制刷新
                crawlProgressRepository.flush();
                
                System.out.println("✅ 进度记录更新成功");
                return savedProgress;
            } else {
                System.err.println("❌ 未找到进度记录ID: " + progressId);
                return null;
            }
        } catch (Exception e) {
            System.err.println("❌ 更新进度记录失败: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，确保事务回滚
        }
    }
    
    /**
     * 更新爬取状态
     */
    @Transactional
    public CrawlProgress updateStatus(Long progressId, String status) {
        Optional<CrawlProgress> optional = crawlProgressRepository.findById(progressId);
        if (optional.isPresent()) {
            CrawlProgress progress = optional.get();
            progress.setStatus(status);
            progress.setUpdatedTime(LocalDateTime.now());
            return crawlProgressRepository.save(progress);
        }
        return null;
    }
    
    /**
     * 获取所有未完成的爬取任务
     */
    public List<CrawlProgress> findIncompleteTasks() {
        return crawlProgressRepository.findByStatusNot("COMPLETED");
    }
    
    /**
     * 删除爬取进度记录
     */
    @Transactional
    public void deleteProgress(Long progressId) {
        crawlProgressRepository.deleteById(progressId);
    }
}
