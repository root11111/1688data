package com.example.demo.repository;

import com.example.demo.entity.CrawlProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlProgressRepository extends JpaRepository<CrawlProgress, Long> {
    
    /**
     * 根据URL查找爬取进度
     */
    Optional<CrawlProgress> findByUrl(String url);
    
    /**
     * 查找所有未完成的任务
     */
    List<CrawlProgress> findByStatusNot(String status);
    
    /**
     * 根据状态查找任务
     */
    List<CrawlProgress> findByStatus(String status);
}
