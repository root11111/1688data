package com.example.demo.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ManufacturerInfo {
    private String companyName;      // 公司名称
    private String contactPerson;    // 联系人
    private String phoneNumber;     // 联系电话
    private String address;         // 地址
    private String mainProducts;    // 主营产品
    private String businessType;    // 经营模式
    private String companyUrl;      // 公司链接
    private String productTitle;    // 产品标题
    private String price;           // 价格信息
    private String minOrder;        // 最小起订量
    private String supplyAbility;   // 供应能力
    private String companyLevel;    // 公司等级
    private String businessLicense; // 营业执照
    private String registeredCapital; // 注册资本
    private String establishmentYear; // 成立年份
    private String employeeCount;   // 员工人数
    private String annualRevenue;   // 年营业额
    private String exportMarket;    // 出口市场
    private String certification;   // 认证信息
    private String contactInfo;     // 联系方式信息（综合）
    private String fax; // 传真
    private LocalDateTime crawlTime; // 爬取时间
    private String sourceUrl;       // 来源URL
    private Integer pageNumber;
    public Integer getPageNumber() {
        return pageNumber;
    }
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}