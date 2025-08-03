package com.example.demo.service;

import com.example.demo.entity.ManufacturerInfo;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    /**
     * 将供应商信息导出到Excel文件
     * @param manufacturers 供应商信息列表
     * @param filePath 文件保存路径
     * @return 是否导出成功
     */
    public boolean exportToExcel(List<ManufacturerInfo> manufacturers, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("1688供应商信息");
            
            // 创建标题行样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 创建标题行
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "公司名称", "联系人", "联系电话", "地址", "主营产品", 
                "经营模式", "产品标题", "价格", "最小起订量", "供应能力",
                "公司等级", "注册资本", "成立年份", "员工人数", "年营业额",
                "出口市场", "认证信息", "公司链接", "爬取时间", "来源URL"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据行
            for (int i = 0; i < manufacturers.size(); i++) {
                ManufacturerInfo info = manufacturers.get(i);
                Row dataRow = sheet.createRow(i + 1);
                
                dataRow.createCell(0).setCellValue(info.getCompanyName() != null ? info.getCompanyName() : "");
                dataRow.createCell(1).setCellValue(info.getContactPerson() != null ? info.getContactPerson() : "");
                dataRow.createCell(2).setCellValue(info.getPhoneNumber() != null ? info.getPhoneNumber() : "");
                dataRow.createCell(3).setCellValue(info.getAddress() != null ? info.getAddress() : "");
                dataRow.createCell(4).setCellValue(info.getMainProducts() != null ? info.getMainProducts() : "");
                dataRow.createCell(5).setCellValue(info.getBusinessType() != null ? info.getBusinessType() : "");
                dataRow.createCell(6).setCellValue(info.getProductTitle() != null ? info.getProductTitle() : "");
                dataRow.createCell(7).setCellValue(info.getPrice() != null ? info.getPrice() : "");
                dataRow.createCell(8).setCellValue(info.getMinOrder() != null ? info.getMinOrder() : "");
                dataRow.createCell(9).setCellValue(info.getSupplyAbility() != null ? info.getSupplyAbility() : "");
                dataRow.createCell(10).setCellValue(info.getCompanyLevel() != null ? info.getCompanyLevel() : "");
                dataRow.createCell(11).setCellValue(info.getRegisteredCapital() != null ? info.getRegisteredCapital() : "");
                dataRow.createCell(12).setCellValue(info.getEstablishmentYear() != null ? info.getEstablishmentYear() : "");
                dataRow.createCell(13).setCellValue(info.getEmployeeCount() != null ? info.getEmployeeCount() : "");
                dataRow.createCell(14).setCellValue(info.getAnnualRevenue() != null ? info.getAnnualRevenue() : "");
                dataRow.createCell(15).setCellValue(info.getExportMarket() != null ? info.getExportMarket() : "");
                dataRow.createCell(16).setCellValue(info.getCertification() != null ? info.getCertification() : "");
                dataRow.createCell(17).setCellValue(info.getCompanyUrl() != null ? info.getCompanyUrl() : "");
                dataRow.createCell(18).setCellValue(info.getCrawlTime() != null ? 
                    info.getCrawlTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                dataRow.createCell(19).setCellValue(info.getSourceUrl() != null ? info.getSourceUrl() : "");
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                System.out.println("Excel文件已保存到: " + filePath);
                return true;
            }
            
        } catch (IOException e) {
            System.err.println("导出Excel文件失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 生成默认的Excel文件名
     * @return 文件名
     */
    public String generateDefaultFileName() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "1688供应商信息_" + timestamp + ".xlsx";
    }
    
    /**
     * 创建标题行样式
     * @param workbook 工作簿
     * @return 单元格样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        
        // 设置字体
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        
        // 设置背景色
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        style.setFont(font);
        return style;
    }
    
    /**
     * 导出到默认路径
     * @param manufacturers 供应商信息列表
     * @return 是否导出成功
     */
    public boolean exportToDefaultPath(List<ManufacturerInfo> manufacturers) {
        try {
            String fileName = generateDefaultFileName();
            String filePath = "exports/" + fileName;
            
            System.out.println("📁 准备导出Excel文件...");
            System.out.println("📄 文件名: " + fileName);
            System.out.println("📂 文件路径: " + filePath);
            
            // 确保导出目录存在
            java.io.File exportDir = new java.io.File("exports");
            if (!exportDir.exists()) {
                boolean created = exportDir.mkdirs();
                System.out.println("📁 创建导出目录: " + (created ? "成功" : "失败"));
            } else {
                System.out.println("📁 导出目录已存在");
            }
            
            boolean result = exportToExcel(manufacturers, filePath);
            if (result) {
                java.io.File file = new java.io.File(filePath);
                System.out.println("📊 文件大小: " + file.length() + " 字节");
                System.out.println("📊 数据条数: " + manufacturers.size());
            }
            return result;
        } catch (Exception e) {
            System.err.println("❌ 导出到默认路径失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 