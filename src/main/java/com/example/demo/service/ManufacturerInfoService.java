package com.example.demo.service;

import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.repository.ManufacturerInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ManufacturerInfoService {
    
    @Autowired
    private ManufacturerInfoRepository manufacturerInfoRepository;
    
    /**
     * 分页查询所有数据
     */
    public Page<ManufacturerInfo> findAll(Pageable pageable) {
        return manufacturerInfoRepository.findAll(pageable);
    }
    
    /**
     * 根据公司名称模糊查询
     */
    public Page<ManufacturerInfo> findByCompanyNameContaining(String companyName, Pageable pageable) {
        return manufacturerInfoRepository.findByCompanyNameContaining(companyName, pageable);
    }
    
    /**
     * 根据页码查询
     */
    public Page<ManufacturerInfo> findByPageNumber(Integer pageNumber, Pageable pageable) {
        return manufacturerInfoRepository.findByPageNumber(pageNumber, pageable);
    }
    
    /**
     * 根据关键词搜索
     */
    public List<ManufacturerInfo> searchByKeyword(String keyword) {
        return manufacturerInfoRepository.searchByKeyword(keyword);
    }
    
    /**
     * 获取总数
     */
    public long count() {
        return manufacturerInfoRepository.count();
    }
    
    /**
     * 按页码统计数量
     */
    public Map<Integer, Long> countByPageNumber() {
        List<Object[]> results = manufacturerInfoRepository.countByPageNumber();
        return results.stream()
                .collect(Collectors.toMap(
                    row -> (Integer) row[0],
                    row -> (Long) row[1]
                ));
    }
    
    /**
     * 根据ID查询
     */
    public ManufacturerInfo findById(Long id) {
        return manufacturerInfoRepository.findById(id).orElse(null);
    }
    
    /**
     * 保存数据
     */
    public ManufacturerInfo save(ManufacturerInfo manufacturerInfo) {
        return manufacturerInfoRepository.save(manufacturerInfo);
    }
    
    /**
     * 删除数据
     */
    public void deleteById(Long id) {
        manufacturerInfoRepository.deleteById(id);
    }

    /**
     * 查询所有数据（不分页）
     */
    public List<ManufacturerInfo> findAll() {
        return manufacturerInfoRepository.findAll();
    }

    /**
     * 根据页码查询所有数据（不分页）
     */
    public List<ManufacturerInfo> findAllByPageNumber(Integer pageNumber) {
        return manufacturerInfoRepository.findByPageNumber(pageNumber);
    }
}
