# MySQL数据库配置指南

## 前置要求

1. **安装MySQL 8.0+**
2. **启动MySQL服务**
3. **创建数据库用户**（可选，默认使用root）

## 配置步骤

### 1. 启动MySQL服务
```bash
# Windows
net start mysql

# Linux/Mac
sudo systemctl start mysql
# 或
sudo service mysql start
```

### 2. 创建数据库和表
```bash
# 连接到MySQL
mysql -u root -p

# 运行初始化脚本
source src/main/resources/db/init.sql
```

或者手动执行：
```sql
CREATE DATABASE IF NOT EXISTS crawler_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE crawler_db;

-- 创建表结构（init.sql中的内容）
```

### 3. 修改数据库连接参数
如果使用不同的用户名/密码，修改 `application.properties`：
```properties
spring.datasource.username=你的用户名
spring.datasource.password=你的密码
```

### 4. 测试MySQL连接
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--test-mysql"
```

### 5. 开始爬取第二页
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--resume"
```

## 数据库结构

### crawl_progress 表（爬取进度）
- `id`: 主键
- `url`: 爬取URL
- `current_page`: 当前页（已设置为2）
- `current_item_index`: 当前商品索引（已设置为0）
- `total_pages`: 总页数（5页）
- `status`: 状态（IN_PROGRESS）
- `last_crawled_time`: 最后爬取时间
- `created_time`: 创建时间
- `updated_time`: 更新时间

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
- `created_time`: 创建时间

## 常见问题

### 1. 连接失败
- 检查MySQL服务是否启动
- 验证用户名密码是否正确
- 确认数据库是否存在

### 2. 表不存在
- 运行 `init.sql` 脚本
- 检查数据库名称是否正确

### 3. 权限问题
```sql
-- 给用户授权
GRANT ALL PRIVILEGES ON crawler_db.* TO '用户名'@'localhost';
FLUSH PRIVILEGES;
```

## 当前状态

✅ **已完成配置**：
- 爬取进度设置为第2页开始
- 商品索引从0开始
- 状态为进行中（IN_PROGRESS）

🚀 **下一步**：
- 运行 `--test-mysql` 测试连接
- 运行 `--resume` 开始爬取第二页
