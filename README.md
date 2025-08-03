# 1688供应商信息爬虫

这是一个基于Spring Boot的1688网站供应商信息爬虫项目，可以自动爬取1688网站上的供应商信息并导出为Excel文件。

## 功能特性

- 🔍 自动爬取1688搜索页面的供应商信息
- 📊 提取详细的供应商信息（公司名称、联系人、电话、地址等）
- 📈 **Excel导出功能** - 自动将爬取数据保存为Excel文件
- 🛡️ 内置反爬虫机制，模拟人类行为
- 🌐 提供RESTful API接口
- 📱 提供Web界面进行测试
- ⚡ 支持多页爬取
- 🔄 自动翻页功能

## 技术栈

- **后端**: Spring Boot 3.5.4
- **爬虫**: Selenium WebDriver
- **浏览器驱动**: ChromeDriver
- **Excel处理**: Apache POI
- **前端**: HTML + CSS + JavaScript
- **构建工具**: Maven

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- Chrome浏览器

### 2. 克隆项目

```bash
git clone <项目地址>
cd demo1
```

### 3. 运行项目

```bash
# 使用Maven运行
mvn spring-boot:run

# 或者先编译再运行
mvn clean package
java -jar target/demo1-0.0.1-SNAPSHOT.jar
```

### 4. 访问应用

启动成功后，访问以下地址：

- **Web界面**: http://localhost:8080
- **API文档**: http://localhost:8080/api/crawler/health

## Excel导出功能

### 自动导出
爬取数据时会自动生成Excel文件，包含完整的供应商信息：

- 公司名称、联系人、联系电话
- 地址、主营产品、经营模式
- 产品标题、价格、最小起订量
- 供应能力、公司等级、注册资本
- 成立年份、员工人数、年营业额
- 出口市场、认证信息、公司链接
- 爬取时间、来源URL

### 文件位置
Excel文件保存在 `exports/` 目录下，文件名格式：
```
1688供应商信息_YYYYMMDD_HHMMSS.xlsx
```

### 使用方法

#### Web界面
1. 访问 http://localhost:8080
2. 勾选"自动导出到Excel文件"选项
3. 输入1688搜索页面URL
4. 点击"开始爬取"
5. 爬取完成后Excel文件自动生成

#### API接口
```bash
# 爬取并导出Excel
curl "http://localhost:8080/api/crawler/1688?url=YOUR_URL&pages=3&exportToExcel=true"

# 单独导出Excel
curl -X POST "http://localhost:8080/api/crawler/export" \
  -H "Content-Type: application/json" \
  -d '[{"companyName": "测试公司", "contactPerson": "张三"}]'
```

## API接口

### 1. 爬取供应商信息

```
GET /api/crawler/1688?url={1688搜索页面URL}&pages={爬取页数}&exportToExcel={是否导出Excel}
```

**参数说明:**
- `url`: 1688搜索页面的完整URL
- `pages`: 要爬取的页数（默认3页，最大10页）
- `exportToExcel`: 是否导出Excel文件（默认false）

**示例:**
```bash
curl "http://localhost:8080/api/crawler/1688?url=https://www.1688.com/zw/page.html?keywords=合肥锂电池组&pages=2&exportToExcel=true"
```

### 2. 测试爬取

```
GET /api/crawler/1688/test?exportToExcel={是否导出Excel}
```

使用预设的URL进行测试爬取。

### 3. 导出Excel

```
POST /api/crawler/export
Content-Type: application/json

[供应商信息数组]
```

### 4. 健康检查

```
GET /api/crawler/health
```

检查爬虫服务状态。

## 返回数据格式

```json
{
  "success": true,
  "message": "爬取成功",
  "data": [
    {
      "companyName": "公司名称",
      "contactPerson": "联系人",
      "phoneNumber": "联系电话",
      "address": "地址",
      "mainProducts": "主营产品",
      "businessType": "经营模式",
      "productTitle": "产品标题",
      "price": "价格信息",
      "minOrder": "最小起订量",
      "supplyAbility": "供应能力",
      "companyLevel": "公司等级",
      "registeredCapital": "注册资本",
      "establishmentYear": "成立年份",
      "employeeCount": "员工人数",
      "annualRevenue": "年营业额",
      "exportMarket": "出口市场",
      "certification": "认证信息",
      "crawlTime": "2024-01-01T12:00:00",
      "sourceUrl": "来源URL"
    }
  ],
  "total": 1,
  "excelFilePath": "exports/1688供应商信息_20250803_232901.xlsx"
}
```

## 使用说明

### 1. Web界面使用

1. 打开浏览器访问 http://localhost:8080
2. 在URL输入框中输入1688搜索页面的URL
3. 设置要爬取的页数
4. **勾选"自动导出到Excel文件"选项**
5. 点击"开始爬取"按钮
6. 等待爬取完成，查看结果和Excel文件

### 2. 命令行使用

```bash
# 测试爬取并导出Excel
curl "http://localhost:8080/api/crawler/1688/test?exportToExcel=true"

# 自定义爬取并导出Excel
curl "http://localhost:8080/api/crawler/1688?url=YOUR_URL&pages=3&exportToExcel=true"
```

## 注意事项

1. **反爬虫机制**: 项目内置了反爬虫机制，包括随机等待、模拟人类行为等
2. **浏览器要求**: 需要安装Chrome浏览器
3. **网络要求**: 确保网络连接正常，能够访问1688网站
4. **使用频率**: 建议控制爬取频率，避免对目标网站造成过大压力
5. **数据准确性**: 爬取的数据仅供参考，实际使用时请验证数据准确性
6. **Excel导出**: 确保项目目录有写入权限，Excel文件会保存在 `exports/` 目录下

## 项目结构

```
demo1/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── controller/
│   │   │   │   └── CrawlerController.java    # API控制器
│   │   │   ├── entity/
│   │   │   │   └── ManufacturerInfo.java     # 供应商信息实体
│   │   │   ├── service/
│   │   │   │   ├── AlibabaCrawlerService.java # 爬虫服务
│   │   │   │   └── ExcelExportService.java   # Excel导出服务
│   │   │   └── Demo1Application.java         # 启动类
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html                # Web界面
│   │       └── application.properties        # 配置文件
│   └── test/
│       └── java/com/example/demo/
│           ├── service/
│           │   ├── AlibabaCrawlerServiceTest.java # 爬虫测试
│           │   └── ExcelExportServiceTest.java    # Excel导出测试
│           ├── controller/
│           │   └── CrawlerControllerTest.java     # 控制器测试
│           └── IntegrationTest.java               # 集成测试
├── exports/                                    # Excel文件导出目录
├── pom.xml                                     # Maven配置
├── README.md                                   # 项目说明
├── TESTING.md                                  # 测试说明
└── EXCEL_EXPORT.md                             # Excel导出说明
```

## 故障排除

### 1. ChromeDriver问题

如果遇到ChromeDriver相关错误，请确保：

- Chrome浏览器已正确安装
- 项目依赖中的WebDriverManager会自动下载匹配的ChromeDriver

### 2. 爬取失败

如果爬取失败，可能的原因：

- 网络连接问题
- 目标网站结构发生变化
- 被反爬虫机制拦截

### 3. Excel导出失败

如果Excel导出失败，可能的原因：

- 目录写入权限不足
- 磁盘空间不足
- 文件被其他程序占用

### 4. 内存不足

如果遇到内存不足问题，可以：

- 减少爬取页数
- 增加JVM内存参数：`-Xmx2g`

## 开发计划

- [x] 添加Excel导出功能
- [ ] 添加数据持久化功能
- [ ] 支持更多电商平台
- [ ] 添加数据过滤功能
- [ ] 优化反爬虫机制
- [ ] 添加定时爬取功能
- [ ] 支持CSV格式导出
- [ ] 添加数据统计分析

## 许可证

本项目仅供学习和研究使用，请遵守相关法律法规和网站使用条款。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。 #   1 6 8 8 d a t a  
 