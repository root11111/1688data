package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_progress")
public class CrawlProgress {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "url", nullable = false, length = 1000)
    private String url;
    
    @Column(name = "current_page", nullable = false)
    private Integer currentPage;
    
    @Column(name = "current_item_index", nullable = false)
    private Integer currentItemIndex;
    
    @Column(name = "total_pages", nullable = false)
    private Integer totalPages;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status; // STARTED, IN_PROGRESS, COMPLETED, FAILED
    
    @Column(name = "last_crawled_time")
    private LocalDateTime lastCrawledTime;
    
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;
    
    // 构造函数
    public CrawlProgress() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }
    
    public CrawlProgress(String url, Integer totalPages) {
        this();
        this.url = url;
        this.totalPages = totalPages;
        this.currentPage = 1;
        this.currentItemIndex = 0;
        this.status = "STARTED";
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
    
    public Integer getCurrentItemIndex() {
        return currentItemIndex;
    }
    
    public void setCurrentItemIndex(Integer currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getLastCrawledTime() {
        return lastCrawledTime;
    }
    
    public void setLastCrawledTime(LocalDateTime lastCrawledTime) {
        this.lastCrawledTime = lastCrawledTime;
    }
    
    public LocalDateTime getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
    
    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
    
    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = LocalDateTime.now();
    }
}
