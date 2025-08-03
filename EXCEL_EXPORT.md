# Excel导出功能使用说明

## 功能概述

本项目已集成Excel导出功能，可以将爬取到的1688供应商信息自动保存为Excel文件，方便数据分析和后续处理。

## 功能特性

- ✅ 自动生成带时间戳的Excel文件名
- ✅ 包含完整的供应商信息字段
- ✅ 美观的表格样式（标题行带颜色）
- ✅ 自动调整列宽
- ✅ 支持空值和null值处理
- ✅ 自动创建导出目录

## Excel文件结构

### 文件命名规则
```
1688供应商信息_YYYYMMDD_HHMMSS.xlsx
```
例如：`1688供应商信息_20250803_232901.xlsx`

### 包含的字段
| 序号 | 字段名 | 说明 |
|------|--------|------|
| 1 | 公司名称 | 供应商公司名称 |
| 2 | 联系人 | 联系人姓名 |
| 3 | 联系电话 | 电话号码 |
| 4 | 地址 | 公司地址 |
| 5 | 主营产品 | 主要经营产品 |
| 6 | 经营模式 | 经营模式（如生产加工等） |
| 7 | 产品标题 | 商品标题 |
| 8 | 价格 | 价格信息 |
| 9 | 最小起订量 | 最小订购数量 |
| 10 | 供应能力 | 供应能力描述 |
| 11 | 公司等级 | 公司等级信息 |
| 12 | 注册资本 | 注册资本 |
| 13 | 成立年份 | 公司成立年份 |
| 14 | 员工人数 | 员工数量 |
| 15 | 年营业额 | 年营业额 |
| 16 | 出口市场 | 出口市场信息 |
| 17 | 认证信息 | 相关认证 |
| 18 | 公司链接 | 公司网址 |
| 19 | 爬取时间 | 数据爬取时间 |
| 20 | 来源URL | 数据来源页面 |

## 使用方法

### 1. Web界面使用

1. 打开浏览器访问 `http://localhost:8080`
2. 在URL输入框中输入1688搜索页面URL
3. 设置爬取页数
4. **勾选"自动导出到Excel文件"选项**
5. 点击"开始爬取"或"测试爬取"
6. 爬取完成后，Excel文件会自动保存到 `exports/` 目录

### 2. API接口使用

#### 爬取并导出Excel
```bash
# 爬取并自动导出Excel
curl "http://localhost:8080/api/crawler/1688?url=YOUR_URL&pages=3&exportToExcel=true"

# 测试爬取并导出Excel
curl "http://localhost:8080/api/crawler/1688/test?exportToExcel=true"
```

#### 单独导出Excel
```bash
# 导出已爬取的数据到Excel
curl -X POST "http://localhost:8080/api/crawler/export" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "companyName": "测试公司",
      "contactPerson": "张三",
      "phoneNumber": "13800138000",
      "address": "北京市朝阳区",
      "mainProducts": "电子产品"
    }
  ]'
```

### 3. 编程方式使用

```java
@Autowired
private ExcelExportService excelExportService;

// 导出到默认路径
List<ManufacturerInfo> manufacturers = // 获取爬取的数据
boolean success = excelExportService.exportToDefaultPath(manufacturers);

// 导出到指定路径
String filePath = "custom_path.xlsx";
boolean success = excelExportService.exportToExcel(manufacturers, filePath);
```

## 文件存储位置

### 默认导出目录
```
项目根目录/exports/
```

### 文件命名示例
```
exports/
├── 1688供应商信息_20250803_232901.xlsx
├── 1688供应商信息_20250803_233015.xlsx
└── 1688供应商信息_20250803_233245.xlsx
```

## API响应格式

### 成功响应
```json
{
  "success": true,
  "message": "爬取成功",
  "data": [...],
  "total": 5,
  "excelFilePath": "exports/1688供应商信息_20250803_232901.xlsx"
}
```

### 失败响应
```json
{
  "success": false,
  "message": "导出失败: 文件写入错误",
  "data": null,
  "total": 0,
  "excelFilePath": null
}
```

## 测试验证

### 运行Excel导出测试
```bash
# 运行所有Excel导出测试
mvn test -Dtest=ExcelExportServiceTest

# 运行特定测试
mvn test -Dtest=ExcelExportServiceTest#testExportToExcel
mvn test -Dtest=ExcelExportServiceTest#testExportToDefaultPath
```

### 测试覆盖范围
- ✅ 文件名生成测试
- ✅ Excel文件导出测试
- ✅ 默认路径导出测试
- ✅ 空数据处理测试
- ✅ null值处理测试

## 配置选项

### 导出目录配置
默认导出目录为 `exports/`，可以通过修改 `ExcelExportService` 中的 `exportToDefaultPath` 方法来更改。

### 文件名格式配置
可以通过修改 `generateDefaultFileName` 方法来自定义文件名格式。

### 表格样式配置
可以通过修改 `createHeaderStyle` 方法来自定义表格样式。

## 故障排除

### 常见问题

#### 1. 文件权限问题
```
错误: 无法创建Excel文件
解决: 检查目录写入权限
```

#### 2. 磁盘空间不足
```
错误: 磁盘空间不足
解决: 清理磁盘空间或更改导出路径
```

#### 3. 文件名冲突
```
错误: 文件已存在
解决: 系统会自动生成带时间戳的文件名，避免冲突
```

### 调试方法

#### 1. 检查导出目录
```bash
# 检查exports目录是否存在
ls -la exports/

# 检查Excel文件是否生成
ls -la exports/*.xlsx
```

#### 2. 查看日志
```bash
# 查看应用日志
tail -f logs/application.log
```

#### 3. 测试Excel文件
```bash
# 使用Excel或LibreOffice打开文件
open exports/1688供应商信息_*.xlsx
```

## 性能优化

### 大数据量处理
- 对于大量数据，建议分批导出
- 可以设置内存参数：`-Xmx2g`

### 并发处理
- 支持多线程同时导出
- 每个导出任务使用独立的Workbook实例

## 扩展功能

### 自定义导出格式
可以扩展 `ExcelExportService` 来支持：
- CSV格式导出
- PDF格式导出
- 自定义模板导出

### 数据过滤
可以在导出前添加数据过滤功能：
- 按地区过滤
- 按产品类型过滤
- 按公司规模过滤

## 联系支持

如有Excel导出相关问题，请：
1. 查看应用日志
2. 运行相关测试
3. 检查文件权限
4. 提交Issue到项目仓库 