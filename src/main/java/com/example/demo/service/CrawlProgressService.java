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
    @Transactional
    public CrawlProgress updateProgress(Long progressId, Integer currentPage, Integer currentItemIndex, String status) {
        Optional<CrawlProgress> optional = crawlProgressRepository.findById(progressId);
        if (optional.isPresent()) {
            CrawlProgress progress = optional.get();
            progress.setCurrentPage(currentPage);
            progress.setCurrentItemIndex(currentItemIndex);
            progress.setStatus(status);
            progress.setLastCrawledTime(LocalDateTime.now());
            progress.setUpdatedTime(LocalDateTime.now());
            return crawlProgressRepository.save(progress);
        }
        return null;
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
