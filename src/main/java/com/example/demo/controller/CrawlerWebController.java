package com.example.demo.controller;

import com.example.demo.entity.CrawlTask;
import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.service.CrawlTaskService;
import com.example.demo.service.ManufacturerInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crawler")
@CrossOrigin(origins = "*")
public class CrawlerWebController {
    
    @Autowired
    private CrawlTaskService crawlTaskService;
    
    @Autowired
    private ManufacturerInfoService manufacturerInfoService;
    
    /**
     * 获取爬取任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<CrawlTask>> getTasks() {
        List<CrawlTask> tasks = crawlTaskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * 获取任务统计信息
     */
    @GetMapping("/tasks/stats")
    public ResponseEntity<Map<String, Object>> getTaskStats() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Object[]> statusCounts = crawlTaskService.getTaskStatistics();
        for (Object[] statusCount : statusCounts) {
            stats.put((String) statusCount[0], statusCount[1]);
        }
        
        stats.put("runningCount", crawlTaskService.getRunningTaskCount());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 创建新的爬取任务
     */
    @PostMapping("/tasks")
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> request) {
        try {
            String taskName = (String) request.get("taskName");
            String url = (String) request.get("url");
            Integer maxPages = Integer.valueOf(request.get("maxPages").toString());
            String description = (String) request.get("description");
            
            CrawlTask task = crawlTaskService.createTask(taskName, url, maxPages, description);
            return ResponseEntity.ok(task);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 启动爬取任务
     */
    @PostMapping("/tasks/{taskId}/start")
    public ResponseEntity<?> startTask(@PathVariable Long taskId) {
        try {
            boolean success = crawlTaskService.startTask(taskId);
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "任务启动成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("任务启动失败");
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 停止爬取任务
     */
    @PostMapping("/tasks/{taskId}/stop")
    public ResponseEntity<?> stopTask(@PathVariable Long taskId) {
        try {
            boolean success = crawlTaskService.stopTask(taskId);
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "任务停止成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("任务停止失败");
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 删除爬取任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        try {
            boolean success = crawlTaskService.deleteTask(taskId);
            if (success) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "任务删除成功");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body("任务删除失败");
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取爬取数据（分页）
     */
    @GetMapping("/data")
    public ResponseEntity<Page<ManufacturerInfo>> getData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) Integer pageNumber) {
        
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<ManufacturerInfo> data;
        if (companyName != null && !companyName.trim().isEmpty()) {
            data = manufacturerInfoService.findByCompanyNameContaining(companyName, pageable);
        } else if (pageNumber != null) {
            data = manufacturerInfoService.findByPageNumber(pageNumber, pageable);
        } else {
            data = manufacturerInfoService.findAll(pageable);
        }
        
        return ResponseEntity.ok(data);
    }
    
    /**
     * 获取数据统计信息
     */
    @GetMapping("/data/stats")
    public ResponseEntity<Map<String, Object>> getDataStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalCount = manufacturerInfoService.count();
        stats.put("totalCount", totalCount);
        
        // 按页码统计
        Map<Integer, Long> pageStats = manufacturerInfoService.countByPageNumber();
        stats.put("pageStats", pageStats);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 搜索数据
     */
    @GetMapping("/data/search")
    public ResponseEntity<List<ManufacturerInfo>> searchData(
            @RequestParam String keyword) {
        
        List<ManufacturerInfo> results = manufacturerInfoService.searchByKeyword(keyword);
        return ResponseEntity.ok(results);
    }
}
