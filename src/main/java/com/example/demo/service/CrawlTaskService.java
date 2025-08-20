package com.example.demo.service;

import com.example.demo.entity.CrawlTask;
import com.example.demo.repository.CrawlTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class CrawlTaskService {
    
    @Autowired
    private CrawlTaskRepository crawlTaskRepository;
    
    @Autowired
    @Lazy
    private AlibabaCrawlerService crawlerService;
    
    // 存储运行中的任务
    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    
    // 定时任务执行器，用于自动重启失败的任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public CrawlTaskService() {
        // 启动定时任务，每10分钟检查一次失败的任务
        startAutoRestartScheduler();
    }
    
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
        
        // 智能进度管理：如果是失败后重启的任务，保持原有进度（断点续传）
        // 如果是新任务或已完成任务重启，则重置进度
        if (task.getCurrentPage() == null || task.getCurrentPage() <= 0) {
            task.setCurrentPage(1);  // 新任务从第1页开始
            task.setCurrentItemIndex(0);  // 新任务从第0项开始
        }
        // 如果 currentPage > 0，说明有进度，保持原有进度（断点续传）
        
        crawlTaskRepository.save(task);
        
        // 异步执行爬取任务
        Future<?> future = taskExecutor.submit(() -> {
            try {
                System.out.println("🚀 开始执行爬取任务: " + task.getTaskName());
                
                // 调用爬虫服务
                crawlerService.crawlManufacturerInfo(task.getUrl(), task.getMaxPages(), task.getId());
                
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
     * 更新任务进度
     */
    @Transactional
    public void updateTaskProgress(Long taskId, Integer currentPage, Integer currentItemIndex) {
        Optional<CrawlTask> optional = crawlTaskRepository.findById(taskId);
        if (optional.isPresent()) {
            CrawlTask task = optional.get();
            task.setCurrentPage(currentPage);
            task.setCurrentItemIndex(currentItemIndex);
            crawlTaskRepository.save(task);
        }
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
    
    /**
     * 启动自动重启调度器
     */
    private void startAutoRestartScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("🔄 自动检查失败任务...");
                autoRestartFailedTasks();
            } catch (Exception e) {
                System.err.println("❌ 自动重启任务检查失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, 10, TimeUnit.MINUTES); // 延迟1分钟启动，然后每10分钟执行一次
        
        System.out.println("✅ 自动重启调度器已启动，每10分钟检查一次失败任务");
    }
    
    /**
     * 自动重启失败的任务
     */
    private void autoRestartFailedTasks() {
        try {
            // 查找所有失败状态的任务
            List<CrawlTask> failedTasks = crawlTaskRepository.findByStatus("FAILED");
            
            if (failedTasks.isEmpty()) {
                System.out.println("✅ 没有发现失败的任务");
                return;
            }
            
            System.out.println("🔍 发现 " + failedTasks.size() + " 个失败的任务，准备自动重启...");
            
            for (CrawlTask failedTask : failedTasks) {
                try {
                    System.out.println("🔄 自动重启失败任务: " + failedTask.getTaskName() + " (ID: " + failedTask.getId() + ")");
                    
                    // 重置任务状态为待处理，但保持原有进度（断点续传）
                    failedTask.setStatus("PENDING");
                    // 不重置进度，保持断点续传
                    // failedTask.setCurrentPage(0);
                    // failedTask.setCurrentItemIndex(0);
                    failedTask.setStartedTime(null);
                    failedTask.setCompletedTime(null);
                    
                    // 保存更新后的任务
                    crawlTaskRepository.save(failedTask);
                    
                    // 自动启动任务
                    startTask(failedTask.getId());
                    
                    System.out.println("✅ 失败任务自动重启成功: " + failedTask.getTaskName());
                    
                    // 等待一小段时间再处理下一个任务，避免同时启动太多任务
                    Thread.sleep(2000);
                    
                } catch (Exception e) {
                    System.err.println("❌ 自动重启任务失败: " + failedTask.getTaskName() + " - " + e.getMessage());
                    // 继续处理下一个任务，不中断整个流程
                }
            }
            
            System.out.println("✅ 自动重启失败任务检查完成");
            
        } catch (Exception e) {
            System.err.println("❌ 自动重启失败任务过程中出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 手动触发失败任务检查（用于测试或紧急情况）
     */
    public void triggerFailedTaskCheck() {
        System.out.println("🚨 手动触发失败任务检查...");
        autoRestartFailedTasks();
    }
    
    /**
     * 关闭服务时清理资源
     */
    public void shutdown() {
        try {
            System.out.println("🔄 正在关闭爬虫任务服务...");
            
            // 停止所有运行中的任务
            for (Long taskId : runningTasks.keySet()) {
                try {
                    stopTask(taskId);
                } catch (Exception e) {
                    System.err.println("❌ 停止任务失败: " + taskId + " - " + e.getMessage());
                }
            }
            
            // 关闭线程池
            taskExecutor.shutdown();
            scheduler.shutdown();
            
            // 等待线程池完全关闭
            if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            System.out.println("✅ 爬虫任务服务已关闭");
            
        } catch (Exception e) {
            System.err.println("❌ 关闭爬虫任务服务时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
