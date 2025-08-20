package com.example.demo.repository;

import com.example.demo.entity.CrawlTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlTaskRepository extends JpaRepository<CrawlTask, Long> {
    
    /**
     * 根据状态查找任务
     */
    List<CrawlTask> findByStatus(String status);
    
    /**
     * 根据任务名称查找
     */
    Optional<CrawlTask> findByTaskName(String taskName);
    
    /**
     * 查找运行中的任务
     */
    @Query("SELECT t FROM CrawlTask t WHERE t.status = 'RUNNING'")
    List<CrawlTask> findRunningTasks();
    
    /**
     * 查找待处理的任务
     */
    @Query("SELECT t FROM CrawlTask t WHERE t.status IN ('PENDING', 'PAUSED')")
    List<CrawlTask> findPendingTasks();
    
    /**
     * 根据URL查找任务
     */
    Optional<CrawlTask> findByUrl(String url);
    
    /**
     * 查找最近创建的任务
     */
    @Query("SELECT t FROM CrawlTask t ORDER BY t.createdTime DESC")
    List<CrawlTask> findRecentTasks();
    
    /**
     * 统计不同状态的任务数量
     */
    @Query("SELECT t.status, COUNT(t) FROM CrawlTask t GROUP BY t.status")
    List<Object[]> countTasksByStatus();
}
