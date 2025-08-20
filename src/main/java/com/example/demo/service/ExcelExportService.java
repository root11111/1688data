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

    private String currentFileName;
    private String exportDirectory;

    /**
     * 获取当前文件名
     * @return 当前文件名
     */
    public String getCurrentFileName() {
        if (currentFileName == null) {
            currentFileName = generateDefaultFileName();
        }
        return currentFileName;
    }

    /**
     * 获取导出目录
     * @return 导出目录
     */
    public String getExportDirectory() {
        if (exportDirectory == null) {
            exportDirectory = new java.io.File("exports").getAbsolutePath();
        }
        return exportDirectory;
    }

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
                    "ID", "公司名称", "产品标题", "联系人", "座机", "手机", "地址", "传真", "页码", "爬取时间", "来源URL"
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

                dataRow.createCell(0).setCellValue(info.getId() != null ? info.getId().toString() : "");
                dataRow.createCell(1).setCellValue(info.getCompanyName() != null ? info.getCompanyName() : "");
                dataRow.createCell(2).setCellValue(info.getProductTitle() != null ? info.getProductTitle() : "");
                dataRow.createCell(3).setCellValue(info.getContactPerson() != null ? info.getContactPerson() : "");
                dataRow.createCell(4).setCellValue(info.getLandlinePhone() != null ? info.getLandlinePhone() : "");
                dataRow.createCell(5).setCellValue(info.getMobilePhone() != null ? info.getMobilePhone() : "");
                dataRow.createCell(6).setCellValue(info.getAddress() != null ? info.getAddress() : "");
                dataRow.createCell(7).setCellValue(info.getFax() != null ? info.getFax() : "");
                dataRow.createCell(8).setCellValue(info.getPageNumber() != null ? info.getPageNumber().toString() : "");
                dataRow.createCell(9).setCellValue(info.getCrawlTime() != null ?
                        info.getCrawlTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                dataRow.createCell(10).setCellValue(info.getSourceUrl() != null ? info.getSourceUrl() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                return true;
            }

        } catch (IOException e) {
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
            System.out.println("📊 Excel导出服务开始...");
            System.out.println("📊 数据条数: " + (manufacturers != null ? manufacturers.size() : "null"));

            String fileName = generateDefaultFileName();
            String filePath = "exports/" + fileName;

            System.out.println("📊 文件名: " + fileName);
            System.out.println("📊 文件路径: " + filePath);

            // 确保导出目录存在
            java.io.File exportDir = new java.io.File("exports");
            System.out.println("📊 检查导出目录: " + exportDir.getAbsolutePath());
            System.out.println("📊 目录是否存在: " + exportDir.exists());

            if (!exportDir.exists()) {
                System.out.println("📊 创建导出目录...");
                boolean created = exportDir.mkdirs();
                System.out.println("📊 目录创建结果: " + created);
                if (!created) {
                    System.err.println("❌ 无法创建导出目录");
                    return false;
                }
            }

            System.out.println("📊 调用exportToExcel方法...");
            boolean result = exportToExcel(manufacturers, filePath);
            System.out.println("📊 exportToExcel返回结果: " + result);

            if (result) {
                java.io.File file = new java.io.File(filePath);
                System.out.println("📊 检查生成的文件: " + file.getAbsolutePath());
                System.out.println("📊 文件是否存在: " + file.exists());
                if (file.exists()) {
                    System.out.println("📊 文件大小: " + file.length() + " 字节");
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("❌ Excel导出服务异常: " + e.getMessage());
            System.err.println("🔍 Excel导出异常详情:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 追加数据到Excel文件（如果文件不存在则创建）
     * @param manufacturer 单个供应商信息
     * @param filePath 文件路径
     * @return 是否成功
     */
    public boolean appendToExcel(ManufacturerInfo manufacturer, String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            Workbook workbook;
            Sheet sheet;

            if (file.exists()) {
                // 文件存在，读取现有文件
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    workbook = new XSSFWorkbook(fis);
                    sheet = workbook.getSheetAt(0);
                }
            } else {
                // 文件不存在，创建新文件
                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet("1688供应商信息");

                // 创建标题行
                CellStyle headerStyle = createHeaderStyle(workbook);
                Row headerRow = sheet.createRow(0);
                String[] headers = {
                        "ID", "公司名称", "产品标题", "联系人", "座机", "手机", "地址", "传真", "页码", "爬取时间", "来源URL"
                };

                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }
            }

            // 添加新数据行
            int lastRowNum = sheet.getLastRowNum();
            Row dataRow = sheet.createRow(lastRowNum + 1);

            dataRow.createCell(0).setCellValue(manufacturer.getId() != null ? manufacturer.getId().toString() : "");
            dataRow.createCell(1).setCellValue(manufacturer.getCompanyName() != null ? manufacturer.getCompanyName() : "");
            dataRow.createCell(2).setCellValue(manufacturer.getProductTitle() != null ? manufacturer.getProductTitle() : "");
            dataRow.createCell(3).setCellValue(manufacturer.getContactPerson() != null ? manufacturer.getContactPerson() : "");
            dataRow.createCell(4).setCellValue(manufacturer.getLandlinePhone() != null ? manufacturer.getLandlinePhone() : "");
            dataRow.createCell(5).setCellValue(manufacturer.getMobilePhone() != null ? manufacturer.getMobilePhone() : "");
            dataRow.createCell(6).setCellValue(manufacturer.getAddress() != null ? manufacturer.getAddress() : "");
            dataRow.createCell(7).setCellValue(manufacturer.getFax() != null ? manufacturer.getFax() : "");
            dataRow.createCell(8).setCellValue(manufacturer.getPageNumber() != null ? manufacturer.getPageNumber().toString() : "");
            dataRow.createCell(9).setCellValue(manufacturer.getCrawlTime() != null ?
                    manufacturer.getCrawlTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            dataRow.createCell(10).setCellValue(manufacturer.getSourceUrl() != null ? manufacturer.getSourceUrl() : "");

            // 保存文件
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                return true;
            } finally {
                workbook.close();
            }

        } catch (IOException e) {
            System.err.println("❌ 追加Excel数据失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 追加数据到默认路径的Excel文件
     * @param manufacturer 单个供应商信息
     * @return 是否成功
     */
    public boolean appendToDefaultPath(ManufacturerInfo manufacturer) {
        try {
            String fileName = getCurrentFileName();
            String filePath = "exports/" + fileName;

            // 确保导出目录存在
            java.io.File exportDir = new java.io.File("exports");
            if (!exportDir.exists()) {
                boolean created = exportDir.mkdirs();
                if (!created) {
                    System.err.println("❌ 无法创建导出目录");
                    return false;
                }
            }

            boolean result = appendToExcel(manufacturer, filePath);
            if (result) {
                System.out.println("✅ 数据已追加到Excel: " + filePath);
                System.out.println("📁 文件位置: " + new java.io.File(filePath).getAbsolutePath());
            }
            return result;
        } catch (Exception e) {
            System.err.println("❌ 追加Excel数据异常: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 