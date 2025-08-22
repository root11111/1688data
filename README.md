# 1688爬虫管理系统

一个基于Spring Boot的智能爬虫系统，支持断点续传、任务管理、数据查询等功能。

## 🚀 主要功能

### 核心功能
- **智能爬取**: 基于Selenium的1688数据爬取
- **断点续传**: 支持程序中断后从断点继续爬取
- **实时监控**: 爬取进度实时更新和监控
- **数据导出**: 支持Excel格式数据导出

### Web管理界面
- **任务管理**: 创建、启动、停止、删除爬取任务
- **数据查询**: 分页查询、关键词搜索、按页码筛选
- **实时统计**: 任务状态统计、数据量统计
- **进度监控**: 实时显示爬取进度和状态

## 🛠️ 技术架构

- **后端**: Spring Boot + Spring Data JPA
- **数据库**: MySQL 8.0+
- **前端**: Bootstrap 5 + 原生JavaScript
- **爬虫**: Selenium WebDriver + WebDriverManager
- **数据处理**: Apache POI (Excel)

## 📋 系统要求

- Java 8+
- MySQL 8.0+
- Maven 3.6+
- Chrome浏览器 (用于爬虫)

## 🚀 快速开始

### 1. 环境准备

#### 启动MySQL服务
```bash
# Windows
net start mysql

# Linux/Mac
sudo systemctl start mysql
```

#### 创建数据库和表
```bash
# 连接到MySQL
mysql -u root -p

# 运行初始化脚本
source src/main/resources/db/init.sql
```

### 2. 配置数据库连接

修改 `src/main/resources/application.properties`：
```properties
spring.datasource.username=你的用户名
spring.datasource.password=你的密码
```

### 3. 启动应用

#### 测试MySQL连接
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--test-mysql"
```

#### 启动Web管理界面
```bash
mvn spring-boot:run
```

访问: http://localhost:8083

## 📖 使用指南

### Web管理界面

#### 1. 任务管理
- **创建任务**: 点击"新建爬取任务"按钮
- **启动任务**: 点击任务卡片上的"启动"按钮
- **停止任务**: 点击任务卡片上的"停止"按钮
- **删除任务**: 点击任务卡片上的"删除"按钮

#### 2. 数据查询
- **分页浏览**: 使用分页控件浏览数据
- **关键词搜索**: 在搜索框输入关键词进行搜索
- **页码筛选**: 选择特定页码查看数据
- **页面大小**: 调整每页显示的数据条数

#### 3. 实时监控
- **任务状态**: 实时显示任务运行状态
- **进度条**: 显示爬取进度百分比
- **统计信息**: 显示任务数量、数据量等统计信息

### 命令行模式

#### 断点续传模式
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--resume"
```

#### 重新开始爬取
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--crawl"
```

## 🗄️ 数据库结构

### crawl_progress 表（爬取进度）
- `id`: 主键
- `url`: 爬取URL
- `current_page`: 当前页
- `current_item_index`: 当前商品索引
- `total_pages`: 总页数
- `status`: 状态
- `last_crawled_time`: 最后爬取时间

### crawl_tasks 表（爬取任务）
- `id`: 主键
- `task_name`: 任务名称（唯一）
- `url`: 爬取URL
- `max_pages`: 最大页数
- `status`: 任务状态
- `current_page`: 当前页
- `current_item_index`: 当前商品索引
- `total_items_crawled`: 已爬取商品数量
- `description`: 任务描述

### manufacturer_info 表（供应商信息）
- `id`: 主键
- `company_name`: 公司名称
- `product_title`: 产品标题
- `price`: 价格
- `contact_person`: 联系人
- `phone_number`: 电话
- `address`: 地址
- `fax`: 传真
- `main_products`: 主营产品
- `contact_info`: 综合联系方式
- `source_url`: 来源URL
- `page_number`: 页码
- `crawl_time`: 爬取时间

## 🔧 配置说明

### 应用配置
```properties
# 服务器端口
server.port=8083

# MySQL数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/crawler_db
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver

# JPA配置
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 爬虫配置
- **反检测**: 随机等待时间、模拟人类行为
- **验证码处理**: 自动检测和处理滑块验证码
- **错误重试**: 网络错误自动重试机制

## 📊 监控和日志

### 实时监控
- 任务运行状态
- 爬取进度百分比
- 数据统计信息
- 错误和异常信息

### 日志记录
- 爬取过程详细日志
- 错误和异常日志
- 性能监控日志

## 🚨 常见问题

### 1. 数据库连接失败
- 检查MySQL服务是否启动
- 验证用户名密码是否正确
- 确认数据库是否存在

### 2. 爬虫无法启动
- 检查Chrome浏览器是否安装
- 确认网络连接正常
- 查看错误日志信息

### 3. 数据查询失败
- 检查数据库表是否存在
- 确认数据是否已爬取
- 验证查询参数是否正确

### 4. 任务管理问题
- 检查任务状态是否正确
- 确认任务权限设置
- 查看任务执行日志

## 🔒 安全注意事项

- 定期备份数据库
- 限制数据库访问权限
- 监控爬虫访问频率
- 遵守网站robots.txt规则

## 📈 性能优化

- 使用连接池管理数据库连接
- 异步执行爬取任务
- 分页查询大数据量
- 定期清理历史数据

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进这个项目！

## 📄 许可证

本项目采用MIT许可证。

---

## 🎯 当前状态

✅ **已完成功能**：
- 爬虫核心功能
- 断点续传机制
- Web管理界面
- 任务管理系统
- 数据查询功能
- MySQL数据库支持

🚀 **下一步计划**：
- 添加更多数据源支持
- 优化爬虫性能
- 增加数据分析功能
- 支持更多导出格式

## 🧪 测试和调试

### 测试验证码检测和进度同步

#### 1. 测试验证码检测
1. 启动项目
2. 访问 `POST /api/crawler/test-unified-update`
3. 查看控制台输出，应该能看到验证码检测和处理的相关日志

#### 2. 测试进度同步
1. 访问 `POST /api/crawler/test-unified-update`
2. 查看控制台输出，应该能看到两个表同步更新的详细日志
3. 检查数据库中 `crawl_progress` 和 `crawl_tasks` 表是否都更新了

#### 3. 测试任务进度更新（新增）
1. 访问 `POST /api/crawler/test-task-update`
2. 查看控制台输出，应该能看到任务进度更新的详细日志
3. 检查数据库中 `crawl_tasks` 表是否更新了
4. 这个测试专门验证 `CrawlTaskService.updateTaskProgress` 方法是否正常工作

#### 4. 测试数据库连接
1. 访问 `GET /api/crawler/test-db`
2. 查看返回结果，确认两个表都能正常访问

#### 5. 测试进度更新（单独）
1. 访问 `POST /api/crawler/test-progress-update`
2. 查看返回结果，确认进度表和任务表都能正常更新

### 调试进度同步问题

如果遇到 `crawl_tasks` 表不更新的问题，可以按以下步骤调试：

1. **检查控制台日志**：查看是否有异常信息
2. **使用测试端点**：依次调用上述测试端点，定位问题
3. **检查数据库**：手动查询数据库确认数据状态
4. **查看事务日志**：检查是否有事务回滚或提交失败

## 🔐 验证码处理改进

### 新特性
- **重试限制**: 验证码处理最多重试3次
- **智能回退**: 失败后返回 FAILED 状态，继续处理下一个商品
- **状态区分**: 区分处理失败和被阻止的情况
- **向后兼容**: 保持与旧版本的兼容性
- **🆕 自动重试机制**: 验证码处理失败时自动重新加载页面或重新进入商品详情页

### 验证码处理结果
- **SUCCESS**: 验证码处理成功，继续正常流程
- **FAILED**: 验证码处理失败，但可以继续爬取下一个商品
- **BLOCKED**: 验证码被阻止，需要人工干预

### 使用场景
- **主页面验证码**: 检测到验证码时尝试处理，失败则重新加载页面
- **页面内验证码**: 每个页面处理前检查验证码，失败则重新加载页面
- **联系方式页面验证码**: 进入联系方式页面时检查验证码，失败则回到主页面重新爬取当前商品
- **翻页后验证码**: 翻页成功后检查新页面验证码，失败则重新翻页

### 优势
1. **不会卡死**: 验证码处理失败时不会一直重试，而是采取相应的回退策略
2. **提高效率**: 避免因单个验证码问题导致整个爬取任务停滞
3. **智能处理**: 根据验证码类型和失败原因采取不同的处理策略
4. **日志清晰**: 详细的日志记录，便于问题排查和监控
5. **🆕 自动恢复**: 验证码处理失败后自动尝试恢复，减少人工干预

### 🆕 新的回退策略
- **主页面验证码失败**: 重新加载页面，重新执行反检测和人类行为模拟
- **页面内验证码失败**: 重新加载页面，重新执行反检测和人类行为模拟
- **联系方式页面验证码失败**: 回到主页面，重新进入商品详情页，重新爬取当前商品信息
- **翻页后验证码失败**: 回到上一页，重新点击下一页按钮

### 测试验证码处理
```bash
POST /api/crawler/test-captcha-handler
```
