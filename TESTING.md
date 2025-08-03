# 1688爬虫项目测试说明

## 测试概述

本项目包含完整的测试套件，用于验证1688爬虫功能的正确性。

## 测试文件结构

```
src/test/java/com/example/demo/
├── service/
│   └── AlibabaCrawlerServiceTest.java    # 爬虫服务测试
├── controller/
│   └── CrawlerControllerTest.java         # 控制器测试
├── IntegrationTest.java                   # 集成测试
├── TestRunner.java                        # 测试运行器
└── Demo1ApplicationTests.java             # 应用启动测试
```

## 测试类型

### 1. 单元测试 (Unit Tests)

#### AlibabaCrawlerServiceTest
- **testManufacturerInfoFields**: 测试实体类字段设置
- **testXPathSelectors**: 验证XPath选择器
- **testCrawlWithInvalidUrl**: 测试无效URL处理
- **testCrawlWithZeroPages**: 测试0页爬取
- **testCrawlManufacturerInfo**: 完整爬取测试（需要网络）

#### CrawlerControllerTest
- **testHealthCheck**: 健康检查端点测试
- **testCrawl1688WithValidUrl**: 有效URL爬取测试
- **testCrawl1688WithMissingUrl**: 缺少URL参数测试
- **testCrawl1688WithException**: 异常处理测试
- **testTestCrawl1688**: 测试爬取端点

### 2. 集成测试 (Integration Tests)

#### IntegrationTest
- **testApplicationContext**: 应用上下文测试
- **testHealthEndpoint**: 健康检查端点集成测试
- **testManufacturerInfoCreation**: 实体创建测试
- **testCrawlerServiceBasicFunctionality**: 爬虫服务基本功能
- **testWebInterfaceAccess**: Web界面访问测试
- **testApiEndpoints**: API端点测试

## 运行测试

### 方法1: 使用Maven命令

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AlibabaCrawlerServiceTest

# 运行特定测试方法
mvn test -Dtest=AlibabaCrawlerServiceTest#testManufacturerInfoFields
```

### 方法2: 使用提供的脚本

#### Windows批处理文件
```bash
run-tests.bat
```

#### PowerShell脚本
```powershell
.\run-tests.ps1
```

### 方法3: 在IDE中运行

1. 在IntelliJ IDEA中打开项目
2. 右键点击测试类或方法
3. 选择"Run Test"

## 测试数据

### 示例供应商信息
- **公司名称**: 深圳市荷力电科技有限公司
- **联系人**: 张洪江先生
- **联系电话**: 13428749846
- **地址**: 广东深圳宝安区石岩街道光明路21号厂房三楼
- **主营产品**: 锂电池组
- **传真**: 86 0769 82193393

### XPath选择器
- 商品列表: `//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]`
- 商品链接: `//a[contains(@href, 'dj.1688.com/ci_bb')]`
- 联系方式按钮: `//a[contains(text(), '联系方式')]`
- 下一页按钮: `//BUTTON[contains(@class,'next-btn next-btn-normal next-btn-large next-pagination-item next')]`

## 测试配置

### 测试环境配置
```properties
# src/test/resources/application-test.properties
spring.main.web-application-type=servlet
logging.level.com.example.demo=DEBUG
crawler.test.mode=true
crawler.test.timeout=5000
test.data.enabled=true
```

### 测试依赖
- JUnit 5
- Spring Boot Test
- Mockito
- Selenium WebDriver

## 测试结果解读

### 成功指标
- 所有测试通过 (Tests run: X, Failures: 0, Errors: 0)
- 实体类字段正确设置
- XPath选择器验证通过
- API端点响应正确
- 异常处理正常工作

### 常见问题

#### 1. Java版本问题
```
错误: 无效的目标发行版: 17
解决: 确保JAVA_HOME指向JDK 17
```

#### 2. Maven编译问题
```
错误: 无法访问org.springframework.boot.SpringApplication
解决: 清理并重新编译项目
```

#### 3. 网络连接问题
```
错误: 连接超时
解决: 检查网络连接，或使用模拟数据测试
```

## 持续集成

### GitHub Actions配置示例
```yaml
name: Test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v2
    - name: Set up JDK 17
      uses: actions/setup-java@v2
      with:
        java-version: '17'
    - name: Run tests
      run: mvn test
```

## 测试覆盖率

建议的测试覆盖率目标：
- 行覆盖率: > 80%
- 分支覆盖率: > 70%
- 方法覆盖率: > 90%

## 性能测试

### 爬取性能指标
- 单页爬取时间: < 30秒
- 内存使用: < 512MB
- 并发支持: 支持多线程爬取

### 压力测试
```bash
# 运行压力测试
mvn test -Dtest=PerformanceTest
```

## 故障排除

### 1. 测试失败排查步骤
1. 检查Java版本: `java -version`
2. 检查Maven版本: `mvn -version`
3. 清理项目: `mvn clean`
4. 重新编译: `mvn compile`
5. 运行测试: `mvn test`

### 2. 日志分析
```bash
# 查看详细测试日志
mvn test -X
```

### 3. 调试模式
```bash
# 启用调试模式
mvn test -Dmaven.surefire.debug
```

## 扩展测试

### 添加新测试
1. 在相应的测试类中添加新的@Test方法
2. 遵循命名规范: `test[功能名称]`
3. 添加适当的断言
4. 更新文档

### 自定义测试数据
```java
@Test
public void testCustomData() {
    // 使用自定义测试数据
    ManufacturerInfo customInfo = createCustomTestData();
    // 执行测试逻辑
}
```

## 联系信息

如有测试相关问题，请：
1. 查看测试日志
2. 检查环境配置
3. 参考本文档
4. 提交Issue到项目仓库 