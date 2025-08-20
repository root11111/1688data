package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "manufacturer_info")
public class ManufacturerInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "company_name", length = 500)
    private String companyName;      // 公司名称
    
    @Column(name = "contact_person", length = 200)
    private String contactPerson;    // 联系人
    
    @Column(name = "phone_number", length = 100)
    private String phoneNumber;     // 联系电话（保留兼容性）
    
    @Column(name = "mobile_phone", length = 100)
    private String mobilePhone;     // 手机号
    
    @Column(name = "landline_phone", length = 100)
    private String landlinePhone;   // 座机号
    
    @Column(name = "address", length = 1000)
    private String address;         // 地址
    
    @Column(name = "main_products", length = 2000)
    private String mainProducts;    // 主营产品
    
    @Column(name = "business_type", length = 200)
    private String businessType;    // 经营模式
    
    @Column(name = "company_url", length = 1000)
    private String companyUrl;      // 公司链接
    
    @Column(name = "product_title", length = 1000)
    private String productTitle;    // 产品标题
    
    @Column(name = "price", length = 200)
    private String price;           // 价格信息
    
    @Column(name = "min_order", length = 200)
    private String minOrder;        // 最小起订量
    
    @Column(name = "supply_ability", length = 500)
    private String supplyAbility;   // 供应能力
    
    @Column(name = "company_level", length = 100)
    private String companyLevel;    // 公司等级
    
    @Column(name = "business_license", length = 500)
    private String businessLicense; // 营业执照
    
    @Column(name = "registered_capital", length = 200)
    private String registeredCapital; // 注册资本
    
    @Column(name = "establishment_year", length = 100)
    private String establishmentYear; // 成立年份
    
    @Column(name = "employee_count", length = 100)
    private String employeeCount;   // 员工人数
    
    @Column(name = "annual_revenue", length = 200)
    private String annualRevenue;   // 年营业额
    
    @Column(name = "export_market", length = 500)
    private String exportMarket;    // 出口市场
    
    @Column(name = "certification", length = 1000)
    private String certification;   // 认证信息
    
    @Column(name = "contact_info", length = 1000)
    private String contactInfo;     // 联系方式信息（综合）
    
    @Column(name = "fax", length = 100)
    private String fax; // 传真
    
    @Column(name = "screenshot_path", length = 1000)
    private String screenshotPath;  // 联系人页面截图路径
    
    @Column(name = "crawl_time")
    private LocalDateTime crawlTime; // 爬取时间
    
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;       // 来源URL
    
    @Column(name = "page_number")
    private Integer pageNumber;
    
    public Integer getPageNumber() {
        return pageNumber;
    }
    
    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getContactPerson() {
        return contactPerson;
    }
    
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getMobilePhone() {
        return mobilePhone;
    }
    
    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }
    
    public String getLandlinePhone() {
        return landlinePhone;
    }
    
    public void setLandlinePhone(String landlinePhone) {
        this.landlinePhone = landlinePhone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getMainProducts() {
        return mainProducts;
    }
    
    public void setMainProducts(String mainProducts) {
        this.mainProducts = mainProducts;
    }
    
    public String getBusinessType() {
        return businessType;
    }
    
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
    
    public String getCompanyUrl() {
        return companyUrl;
    }
    
    public void setCompanyUrl(String companyUrl) {
        this.companyUrl = companyUrl;
    }
    
    public String getProductTitle() {
        return productTitle;
    }
    
    public void setProductTitle(String productTitle) {
        this.productTitle = productTitle;
    }
    
    public String getPrice() {
        return price;
    }
    
    public void setPrice(String price) {
        this.price = price;
    }
    
    public String getMinOrder() {
        return minOrder;
    }
    
    public void setMinOrder(String minOrder) {
        this.minOrder = minOrder;
    }
    
    public String getSupplyAbility() {
        return supplyAbility;
    }
    
    public void setSupplyAbility(String supplyAbility) {
        this.supplyAbility = supplyAbility;
    }
    
    public String getCompanyLevel() {
        return companyLevel;
    }
    
    public void setCompanyLevel(String companyLevel) {
        this.companyLevel = companyLevel;
    }
    
    public String getBusinessLicense() {
        return businessLicense;
    }
    
    public void setBusinessLicense(String businessLicense) {
        this.businessLicense = businessLicense;
    }
    
    public String getRegisteredCapital() {
        return registeredCapital;
    }
    
    public void setRegisteredCapital(String registeredCapital) {
        this.registeredCapital = registeredCapital;
    }
    
    public String getEstablishmentYear() {
        return establishmentYear;
    }
    
    public void setEstablishmentYear(String establishmentYear) {
        this.establishmentYear = establishmentYear;
    }
    
    public String getEmployeeCount() {
        return employeeCount;
    }
    
    public void setEmployeeCount(String employeeCount) {
        this.employeeCount = employeeCount;
    }
    
    public String getAnnualRevenue() {
        return annualRevenue;
    }
    
    public void setAnnualRevenue(String annualRevenue) {
        this.annualRevenue = annualRevenue;
    }
    
    public String getExportMarket() {
        return exportMarket;
    }
    
    public void setExportMarket(String exportMarket) {
        this.exportMarket = exportMarket;
    }
    
    public String getCertification() {
        return certification;
    }
    
    public void setCertification(String certification) {
        this.certification = certification;
    }
    
    public String getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public String getFax() {
        return fax;
    }
    
    public void setFax(String fax) {
        this.fax = fax;
    }
    
    public String getScreenshotPath() {
        return screenshotPath;
    }
    
    public void setScreenshotPath(String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }
    
    public LocalDateTime getCrawlTime() {
        return crawlTime;
    }
    
    public void setCrawlTime(LocalDateTime crawlTime) {
        this.crawlTime = crawlTime;
    }
    
    public String getSourceUrl() {
        return sourceUrl;
    }
    
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}