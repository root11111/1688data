package com.example.demo.service;

import com.example.demo.entity.CrawlTask;
import com.example.demo.repository.CrawlTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class CrawlTaskService {
    
    @Autowired
    private CrawlTaskRepository crawlTaskRepository;
    
    @Autowired
    private AlibabaCrawlerService crawlerService;
    
    // 存储运行中的任务
    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    
    /**
     * 创建新的爬取任务
     */
    @Transactional
    public CrawlTask createTask(String taskName, String url, Integer maxPages, String description) {
        // 检查任务名称是否已存在
        if (crawlTaskRepository.findByTaskName(taskName).isPresent()) {
            throw new RuntimeException("任务名称已存在: " + taskName);
        }
        
        // 检查URL是否已存在
        if (crawlTaskRepository.findByUrl(url).isPresent()) {
            throw new RuntimeException("URL已存在: " + url);
        }
        
        CrawlTask task = new CrawlTask(taskName, url, maxPages);
        task.setDescription(description);
        
        return crawlTaskRepository.save(task);
    }
    
    /**
     * 启动爬取任务
     */
    @Transactional
    public boolean startTask(Long taskId) {
        Optional<CrawlTask> optional = crawlTaskRepository.findById(taskId);
        if (optional.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        CrawlTask task = optional.get();
        
        // 检查任务状态
        if ("RUNNING".equals(task.getStatus())) {
            throw new RuntimeException("任务已在运行中: " + task.getTaskName());
        }
        
        if ("COMPLETED".equals(task.getStatus())) {
            throw new RuntimeException("任务已完成: " + task.getTaskName());
        }
        
        // 更新任务状态
        task.setStatus("RUNNING");
        task.setStartedTime(LocalDateTime.now());
        crawlTaskRepository.save(task);
        
        // 异步执行爬取任务
        Future<?> future = taskExecutor.submit(() -> {
            try {
                System.out.println("🚀 开始执行爬取任务: " + task.getTaskName());
                
                // 调用爬虫服务
                crawlerService.crawlManufacturerInfo(task.getUrl(), task.getMaxPages());
                
                // 任务完成
                task.setStatus("COMPLETED");
                task.setCompletedTime(LocalDateTime.now());
                crawlTaskRepository.save(task);
                
                System.out.println("✅ 爬取任务完成: " + task.getTaskName());
                
            } catch (Exception e) {
                System.err.println("❌ 爬取任务失败: " + task.getTaskName() + " - " + e.getMessage());
                e.printStackTrace();
                
                // 任务失败
                task.setStatus("FAILED");
                crawlTaskRepository.save(task);
            } finally {
                // 从运行中任务列表中移除
                runningTasks.remove(taskId);
            }
        });
        
        runningTasks.put(taskId, future);
        return true;
    }
    
    /**
     * 停止爬取任务
     */
    @Transactional
    public boolean stopTask(Long taskId) {
        Optional<CrawlTask> optional = crawlTaskRepository.findById(taskId);
        if (optional.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        CrawlTask task = optional.get();
        
        if (!"RUNNING".equals(task.getStatus())) {
            throw new RuntimeException("任务未在运行: " + task.getTaskName());
        }
        
        // 取消任务
        Future<?> future = runningTasks.get(taskId);
        if (future != null) {
            future.cancel(true);
            runningTasks.remove(taskId);
        }
        
        // 更新任务状态
        task.setStatus("PAUSED");
        crawlTaskRepository.save(task);
        
        return true;
    }
    
    /**
     * 删除任务
     */
    @Transactional
    public boolean deleteTask(Long taskId) {
        Optional<CrawlTask> optional = crawlTaskRepository.findById(taskId);
        if (optional.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        
        CrawlTask task = optional.get();
        
        // 如果任务正在运行，先停止
        if ("RUNNING".equals(task.getStatus())) {
            stopTask(taskId);
        }
        
        crawlTaskRepository.deleteById(taskId);
        return true;
    }
    
    /**
     * 获取所有任务
     */
    public List<CrawlTask> getAllTasks() {
        return crawlTaskRepository.findRecentTasks();
    }
    
    /**
     * 根据状态获取任务
     */
    public List<CrawlTask> getTasksByStatus(String status) {
        return crawlTaskRepository.findByStatus(status);
    }
    
    /**
     * 获取运行中的任务
     */
    public List<CrawlTask> getRunningTasks() {
        return crawlTaskRepository.findRunningTasks();
    }
    
    /**
     * 获取待处理的任务
     */
    public List<CrawlTask> getPendingTasks() {
        return crawlTaskRepository.findPendingTasks();
    }
    
    /**
     * 根据ID获取任务
     */
    public Optional<CrawlTask> getTaskById(Long taskId) {
        return crawlTaskRepository.findById(taskId);
    }
    
    /**
     * 获取任务统计信息
     */
    public List<Object[]> getTaskStatistics() {
        return crawlTaskRepository.countTasksByStatus();
    }
    
    /**
     * 检查任务是否正在运行
     */
    public boolean isTaskRunning(Long taskId) {
        return runningTasks.containsKey(taskId);
    }
    
    /**
     * 获取运行中任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
}
