package com.example.demo.service;

import com.example.demo.entity.CrawlTask;
import com.example.demo.repository.CrawlTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    
    @PersistenceContext
    private EntityManager entityManager;
    
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
            System.out.println("🔄 新任务或重置任务，设置初始进度: 第1页，第0项");
        } else {
            // 如果 currentPage > 0，说明有进度，保持原有进度（断点续传）
            System.out.println("🔄 断点续传任务，保持原有进度: 第" + task.getCurrentPage() + "页，第" + task.getCurrentItemIndex() + "项");
        }
        
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
     * 更新任务进度 - 重写版本，使用更直接的方式确保数据保存
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTaskProgress(Long taskId, Integer currentPage, Integer currentItemIndex) {
        try {
            System.out.println("🔄 CrawlTaskService.updateTaskProgress 被调用:");
            System.out.println("   - 任务ID: " + taskId);
            System.out.println("   - 目标页码: " + currentPage);
            System.out.println("   - 目标项索引: " + currentItemIndex);
            
            // 🆕 添加参数验证
            if (taskId == null) {
                System.err.println("❌ 任务ID为null");
                return;
            }
            if (currentPage == null) {
                System.err.println("❌ 目标页码为null");
                return;
            }
            if (currentItemIndex == null) {
                System.err.println("❌ 目标项索引为null");
                return;
            }
            
            // 先查询一次，确认当前状态
            Optional<CrawlTask> beforeOptional = crawlTaskRepository.findById(taskId);
            if (beforeOptional.isPresent()) {
                CrawlTask beforeTask = beforeOptional.get();
                System.out.println("🔍 更新前数据库状态:");
                System.out.println("   - 任务名称: " + beforeTask.getTaskName());
                System.out.println("   - 当前页码: " + beforeTask.getCurrentPage());
                System.out.println("   - 当前项索引: " + beforeTask.getCurrentItemIndex());
            }
            
            // 🆕 使用 EntityManager 直接更新，确保数据被正确保存
            String updateQuery = "UPDATE CrawlTask t SET t.currentPage = :currentPage, t.currentItemIndex = :currentItemIndex, t.updatedTime = :updatedTime WHERE t.id = :taskId";
            int updatedRows = entityManager.createQuery(updateQuery)
                    .setParameter("currentPage", currentPage)
                    .setParameter("currentItemIndex", currentItemIndex)
                    .setParameter("updatedTime", LocalDateTime.now())
                    .setParameter("taskId", taskId)
                    .executeUpdate();
            
            System.out.println("🔄 直接SQL更新执行结果:");
            System.out.println("   - 更新的行数: " + updatedRows);
            
            if (updatedRows > 0) {
                // 强制刷新持久化上下文
                entityManager.flush();
                System.out.println("✅ EntityManager flush 完成");

            } else {
                System.err.println("❌ SQL更新未影响任何行，可能任务不存在");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 更新任务进度失败: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，确保事务回滚
        }
    }
    
    /**
     * 🆕 测试方法：验证两个表的同步更新
     */
    public void testSyncUpdate(Long taskId) {
        try {
            System.out.println("🧪 开始测试两个表的同步更新...");
            
            // 获取当前任务状态
            Optional<CrawlTask> taskOpt = getTaskById(taskId);
            if (taskOpt.isPresent()) {
                CrawlTask task = taskOpt.get();
                System.out.println("🔍 测试前任务状态:");
                System.out.println("   - 任务名称: " + task.getTaskName());
                System.out.println("   - 当前页码: " + task.getCurrentPage());
                System.out.println("   - 当前项索引: " + task.getCurrentItemIndex());
                
                // 测试更新 - 使用不同的值
                int testPage = (task.getCurrentPage() != null ? task.getCurrentPage() : 1) + 1;
                int testItemIndex = (task.getCurrentItemIndex() != null ? task.getCurrentItemIndex() : 0) + 1;
                
                System.out.println("🧪 测试更新到: 第" + testPage + "页，第" + testItemIndex + "项");
                updateTaskProgress(taskId, testPage, testItemIndex);
                
                // 等待一下，确保事务提交
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                
                // 验证更新结果
                Optional<CrawlTask> updatedTaskOpt = getTaskById(taskId);
                if (updatedTaskOpt.isPresent()) {
                    CrawlTask updatedTask = updatedTaskOpt.get();
                    System.out.println("🔍 测试后任务状态:");
                    System.out.println("   - 任务名称: " + updatedTask.getTaskName());
                    System.out.println("   - 当前页码: " + updatedTask.getCurrentPage());
                    System.out.println("   - 当前项索引: " + updatedTask.getCurrentItemIndex());
                    
                    if (updatedTask.getCurrentPage().equals(testPage) && updatedTask.getCurrentItemIndex().equals(testItemIndex)) {
                        System.out.println("✅ 测试成功！两个表同步更新正常");
                    } else {
                        System.err.println("❌ 测试失败！两个表同步更新异常");
                        System.err.println("   - 期望页码: " + testPage + ", 实际: " + updatedTask.getCurrentPage());
                        System.err.println("   - 期望项索引: " + testItemIndex + ", 实际: " + updatedTask.getCurrentItemIndex());
                    }
                }
            } else {
                System.err.println("❌ 测试失败！找不到任务ID: " + taskId);
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 🆕 强制刷新数据库状态
     */
    public void forceRefreshDatabase() {
        try {
            System.out.println("🔄 强制刷新数据库状态...");
            // 强制刷新所有未提交的更改
            crawlTaskRepository.flush();
            System.out.println("✅ 数据库状态刷新完成");
        } catch (Exception e) {
            System.err.println("❌ 刷新数据库状态失败: " + e.getMessage());
            e.printStackTrace();
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
