package com.example.demo.repository;

import com.example.demo.entity.ManufacturerInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturerInfoRepository extends JpaRepository<ManufacturerInfo, Long> {
    
    /**
     * 根据公司名称模糊查询
     */
    Page<ManufacturerInfo> findByCompanyNameContaining(String companyName, Pageable pageable);
    
    /**
     * 根据页码查询
     */
    Page<ManufacturerInfo> findByPageNumber(Integer pageNumber, Pageable pageable);
    
    /**
     * 根据关键词搜索（多字段）
     */
    @Query("SELECT m FROM ManufacturerInfo m WHERE " +
           "m.companyName LIKE %:keyword% OR " +
           "m.productTitle LIKE %:keyword% OR " +
           "m.contactPerson LIKE %:keyword% OR " +
           "m.phoneNumber LIKE %:keyword% OR " +
           "m.address LIKE %:keyword% OR " +
           "m.mainProducts LIKE %:keyword%")
    List<ManufacturerInfo> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 按页码统计数量
     */
    @Query("SELECT m.pageNumber, COUNT(m) FROM ManufacturerInfo m GROUP BY m.pageNumber ORDER BY m.pageNumber")
    List<Object[]> countByPageNumber();
    
    /**
     * 获取所有页码
     */
    @Query("SELECT DISTINCT m.pageNumber FROM ManufacturerInfo m ORDER BY m.pageNumber")
    List<Integer> findAllPageNumbers();
}
