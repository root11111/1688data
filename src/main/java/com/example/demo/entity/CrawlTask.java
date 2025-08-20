package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crawl_tasks")
public class CrawlTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_name", nullable = false)
    private String taskName;
    
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;
    
    @Column(name = "max_pages")
    private Integer maxPages;
    
    @Column(name = "status")
    private String status; // PENDING, RUNNING, PAUSED, COMPLETED, FAILED
    
    @Column(name = "current_page")
    private Integer currentPage;
    
    @Column(name = "current_item_index")
    private Integer currentItemIndex;
    
    @Column(name = "total_items_crawled")
    private Integer totalItemsCrawled;
    
    @Column(name = "created_time")
    private LocalDateTime createdTime;
    
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
    
    @Column(name = "started_time")
    private LocalDateTime startedTime;
    
    @Column(name = "completed_time")
    private LocalDateTime completedTime;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    // 构造函数
    public CrawlTask() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        this.status = "PENDING";
        this.currentPage = 1;
        this.currentItemIndex = 0;
        this.totalItemsCrawled = 0;
    }
    
    public CrawlTask(String taskName, String url, Integer maxPages) {
        this();
        this.taskName = taskName;
        this.url = url;
        this.maxPages = maxPages;
    }
    
    // Getter和Setter方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTaskName() {
        return taskName;
    }
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Integer getMaxPages() {
        return maxPages;
    }
    
    public void setMaxPages(Integer maxPages) {
        this.maxPages = maxPages;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
        this.updatedTime = LocalDateTime.now();
    }
    
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
        this.updatedTime = LocalDateTime.now();
    }
    
    public Integer getCurrentItemIndex() {
        return currentItemIndex;
    }
    
    public void setCurrentItemIndex(Integer currentItemIndex) {
        this.currentItemIndex = currentItemIndex;
        this.updatedTime = LocalDateTime.now();
    }
    
    public Integer getTotalItemsCrawled() {
        return totalItemsCrawled;
    }
    
    public void setTotalItemsCrawled(Integer totalItemsCrawled) {
        this.totalItemsCrawled = totalItemsCrawled;
        this.updatedTime = LocalDateTime.now();
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
        this.updatedTime = updatedTime;
    }
    
    public LocalDateTime getStartedTime() {
        return startedTime;
    }
    
    public void setStartedTime(LocalDateTime startedTime) {
        this.startedTime = startedTime;
    }
    
    public LocalDateTime getCompletedTime() {
        return completedTime;
    }
    
    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
