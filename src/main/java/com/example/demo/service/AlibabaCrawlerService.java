package com.example.demo.service;

import com.example.demo.entity.ManufacturerInfo;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class AlibabaCrawlerService {

    @Autowired
    private CaptchaHandlerService captchaHandler;

    @Autowired
    private AntiDetectionService antiDetectionService;

    public List<ManufacturerInfo> crawlManufacturerInfo(String url, int maxPages) {
        // 设置WebDriver
        WebDriverManager.chromedriver().setup();

        // 使用增强的反检测配置
        ChromeOptions options = antiDetectionService.getEnhancedChromeOptions();
        
        // 如果需要无头模式，取消下面这行注释
        // options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        List<ManufacturerInfo> manufacturerInfos = new ArrayList<>();

        try {
            // 1. 打开网页
            System.out.println("正在访问页面: " + url);
            driver.get(url);
            
            // 执行反检测脚本
            antiDetectionService.executeAntiDetectionScripts(driver);
            
            // 等待页面加载
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
            
            // 模拟人类行为
            antiDetectionService.simulateHumanBehavior(driver);
            
            // 随机等待，模拟人类行为
            antiDetectionService.randomWait(2000, 5000);

            // 2. 循环翻页
            for (int page = 1; page <= maxPages; page++) {
                System.out.println("正在处理第 " + page + " 页...");

                // 3. 滚动网页
                scrollPage(driver);

                // 4. 获取商品列表 - 使用八爪鱼的方式：不固定元素列表，动态获取
                List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                System.out.println("找到 " + items.size() + " 个商品");
                
                for (int i = 0; i < items.size(); i++) {
                    try {
                        // 重新获取元素列表，防止StaleElementReferenceException
                        items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                        if (i >= items.size()) break;
                        
                        WebElement item = items.get(i);
                        
                        // 提取商品基本信息
                        ManufacturerInfo info = extractBasicInfo(item, url, driver);
                        
                        // 5. 点击列表链接进入详情页
                        String mainWindow = driver.getWindowHandle();
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", item);
                        antiDetectionService.randomWait(1000, 2000);

                        // 按照八爪鱼的方式：点击列表链接
                        System.out.println("🖱️ 尝试点击第 " + (i + 1) + " 个商品链接...");
                        
                        // 在商品卡片中查找链接元素
                        WebElement linkElement = item.findElement(By.xpath(".//a[contains(@href, 'dj.1688.com/ci_bb')]"));
                        
                        // 直接点击链接元素
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", linkElement);
                        antiDetectionService.randomWait(2000, 4000);
                        
                        // 检查是否打开了新页面
                        if (driver.getWindowHandles().size() > 1) {
                            // 切换到新标签页
                            for (String windowHandle : driver.getWindowHandles()) {
                                if (!windowHandle.equals(mainWindow)) {
                                    driver.switchTo().window(windowHandle);
                                    break;
                                }
                            }
                            
                            System.out.println("✅ 成功打开商品详情页");
                        } else {
                            System.out.println("⚠️ 点击链接未打开新页面，尝试获取链接直接打开");
                            // 尝试获取链接直接打开
                            String productUrl = getProductUrl(item);
                            if (productUrl != null && !productUrl.isEmpty()) {
                                ((JavascriptExecutor) driver).executeScript("window.open(arguments[0]);", productUrl);
                                
                                // 切换到新标签页
                                for (String windowHandle : driver.getWindowHandles()) {
                                    if (!windowHandle.equals(mainWindow)) {
                                        driver.switchTo().window(windowHandle);
                                        break;
                                    }
                                }
                            } else {
                                System.err.println("❌ 无法获取商品链接，跳过此商品");
                                continue;
                            }
                        }

                        // 切换到新标签页
                        for (String windowHandle : driver.getWindowHandles()) {
                            if (!windowHandle.equals(mainWindow)) {
                                driver.switchTo().window(windowHandle);
                                break;
                            }
                        }

                        // 等待新页面加载
                        antiDetectionService.randomWait(2000, 4000);
                        
                        // 检查详情页是否有验证码
                        if (captchaHandler.checkForCaptcha(driver)) {
                            System.out.println("⚠️  详情页检测到验证码！");
                            if (!captchaHandler.handleCaptcha(driver)) {
                                captchaHandler.waitForManualCaptcha();
                            }
                        }

                        // 等待新页面加载并提取详细信息
                        try {
                            // 等待联系方式按钮出现
                            WebElement contactButton = wait.until(ExpectedConditions.elementToBeClickable(
                                By.xpath("//a[contains(text(), '联系方式')]")));
                            
                            // 6. 点击联系方式按钮
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", contactButton);
                            antiDetectionService.randomWait(2000, 4000);
                            
                            // 再次检查点击联系方式后是否出现验证码
                            if (captchaHandler.checkForCaptcha(driver)) {
                                System.out.println("⚠️  点击联系方式后检测到验证码！");
                                if (!captchaHandler.handleCaptcha(driver)) {
                                    captchaHandler.waitForManualCaptcha();
                                }
                            }
                            
                            // 7. 提取联系方式数据
                            extractContactInfo(driver, info);
                            
                        } catch (Exception e) {
                            System.err.println("提取详细信息失败: " + e.getMessage());
                        }

                        // 关闭当前标签页，切换回主窗口
                        driver.close();
                        driver.switchTo().window(mainWindow);

                        // 即使没有成功进入详情页，也保存基本信息
                        manufacturerInfos.add(info);
                        System.out.println("✅ 成功提取第 " + (i + 1) + " 个商品信息: " + info.getCompanyName());
                        System.out.println("   📝 商品标题: " + info.getProductTitle());
                        System.out.println("   💰 价格: " + info.getPrice());
                        System.out.println("   📞 联系方式: " + info.getContactInfo());

                        // 防止被封，随机等待 - 增加等待时间
                        antiDetectionService.randomWait(5000, 12000);

                    } catch (Exception e) {
                        System.err.println("处理第 " + i + " 个商品时出错: " + e.getMessage());
                        continue;
                    }
                }

                // 尝试翻页 - 使用您提供的XPath
                if (page < maxPages) {
                    if (!tryNextPage(driver, wait)) {
                        System.out.println("没有更多页面了");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("爬取过程中出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        System.out.println("爬取完成，共获取 " + manufacturerInfos.size() + " 条供应商信息");
        return manufacturerInfos;
    }

    private String getProductUrl(WebElement item) {
        try {
            // 使用JavaScript获取链接，避免堆栈溢出
            WebElement link = item.findElement(By.xpath(".//a[contains(@href, 'dj.1688.com/ci_bb')]"));
            
            // 直接尝试获取href属性，如果失败则跳过
            try {
                String href = link.getAttribute("href");
                if (href != null && !href.isEmpty()) {
                    System.out.println("🔍 找到商品链接: " + href);
                    return href;
                }
            } catch (Exception e) {
                System.out.println("⚠️ 获取href属性失败，跳过此商品");
            }
            
            System.out.println("❌ 未找到有效的商品链接");
            return null;
        } catch (Exception e) {
            System.err.println("获取商品链接时出错: " + e.getMessage());
            return null;
        }
    }

    private ManufacturerInfo extractBasicInfo(WebElement item, String sourceUrl, WebDriver driver) {
        ManufacturerInfo info = new ManufacturerInfo();
        info.setCrawlTime(LocalDateTime.now());
        info.setSourceUrl(sourceUrl);

        try {
            // 提取商品标题 - 使用JavaScript避免堆栈溢出
            WebElement titleElement = item.findElement(By.xpath(".//div[@class='offer-title']/span"));
            // 直接使用getText()方法，如果失败则使用JavaScript
            String productTitle;
            try {
                productTitle = titleElement.getText().trim();
            } catch (Exception e) {
                // 如果getText()失败，使用JavaScript
                productTitle = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", titleElement);
                productTitle = productTitle.trim();
            }
            if (!productTitle.isEmpty()) {
                info.setProductTitle(productTitle);
                System.out.println("📝 提取到商品标题: " + productTitle);
            } else {
                info.setProductTitle("未获取到商品标题");
            }
        } catch (Exception e) {
            info.setProductTitle("未获取到商品标题");
            System.err.println("❌ 提取商品标题失败: " + e.getMessage());
        }

        try {
            // 提取价格信息 - 使用JavaScript避免堆栈溢出
            WebElement priceElement = item.findElement(By.xpath(".//div[@class='offer-price']/span[@class='price']"));
            // 直接使用getText()方法，如果失败则使用JavaScript
            String price;
            try {
                price = priceElement.getText().trim();
            } catch (Exception e) {
                // 如果getText()失败，使用JavaScript
                price = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", priceElement);
                price = price.trim();
            }
            if (!price.isEmpty()) {
                info.setPrice(price);
                System.out.println("💰 提取到价格: " + price);
            } else {
                info.setPrice("未获取到价格信息");
            }
        } catch (Exception e) {
            info.setPrice("未获取到价格信息");
            System.err.println("❌ 提取价格失败: " + e.getMessage());
        }

        // 公司名称将在详情页的联系方式中获取
        info.setCompanyName("待从详情页获取");

        return info;
    }

    private void extractContactInfo(WebDriver driver, ManufacturerInfo info) {
        // 首先等待页面加载完成
        System.out.println("⏳ 等待联系方式页面加载...");
        try {
            Thread.sleep(3000); // 等待3秒让页面完全加载
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 调试：打印页面标题和URL
        System.out.println("📄 当前页面标题: " + driver.getTitle());
        System.out.println("🔗 当前页面URL: " + driver.getCurrentUrl());
        
        // 保存页面HTML代码到文件，用于分析页面结构
        try {
            String pageSource = driver.getPageSource();
            String fileName = "contact_page_" + System.currentTimeMillis() + ".html";
            java.nio.file.Files.write(java.nio.file.Paths.get(fileName), pageSource.getBytes("UTF-8"));
            System.out.println("💾 已保存页面HTML到文件: " + fileName);
            
            // 同时打印页面中所有包含联系方式的文本
            System.out.println("🔍 搜索页面中的联系方式信息...");
            List<WebElement> allElements = driver.findElements(By.xpath("//*[contains(text(), '电话') or contains(text(), '手机') or contains(text(), '地址') or contains(text(), '传真') or contains(text(), '联系人') or contains(text(), '公司')]"));
            System.out.println("📋 找到 " + allElements.size() + " 个包含联系方式的元素:");
            for (int i = 0; i < Math.min(allElements.size(), 20); i++) {
                try {
                    String text = allElements.get(i).getText().trim();
                    if (!text.isEmpty() && text.length() > 2) {
                        System.out.println("   " + (i + 1) + ". " + text);
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
            
            // 额外分析：查找所有div元素，看看实际的HTML结构
            System.out.println("🔍 分析页面中的div元素结构...");
            List<WebElement> allDivs = driver.findElements(By.xpath("//div"));
            System.out.println("📋 找到 " + allDivs.size() + " 个div元素");
            
            // 查找包含特定文本的div元素
            for (WebElement div : allDivs) {
                try {
                    String text = div.getText().trim();
                    if (text.contains("电话") || text.contains("手机") || text.contains("地址") || text.contains("传真") || text.contains("联系人")) {
                        System.out.println("📋 找到包含联系方式的div: " + text);
                        // 获取该div的HTML属性
                        String style = div.getAttribute("style");
                        String className = div.getAttribute("class");
                        System.out.println("   style: " + style);
                        System.out.println("   class: " + className);
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 保存页面HTML失败: " + e.getMessage());
        }
        
        // 根据八爪鱼任务，使用更精确的XPath选择器
        System.out.println("🔍 使用八爪鱼方式提取联系方式信息...");
        
        try {
            // 根据八爪鱼任务，使用更精确的XPath选择器提取公司名称
            System.out.println("🔍 尝试查找公司名称元素...");
            // 使用八爪鱼任务中的XPath：//div[contains(@style, 'font-size: 20px') and contains(@style, 'color: rgb(51, 51, 51)')]
            WebElement companyElement = driver.findElement(By.xpath("//div[contains(@style, 'font-size: 20px') and contains(@style, 'color: rgb(51, 51, 51)')]"));
            if (companyElement != null) {
                String companyName = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", companyElement);
                companyName = companyName.trim();
                if (!companyName.isEmpty() && !companyName.equals("联系方式")) {
                    info.setCompanyName(companyName);
                    System.out.println("🏢 提取到公司名称: " + companyName);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 提取公司名称失败: " + e.getMessage());
            // 尝试备用方法
            try {
                System.out.println("🔄 尝试备用方法查找公司名称...");
                List<WebElement> allDivs = driver.findElements(By.xpath("//div"));
                for (WebElement div : allDivs) {
                    try {
                        String text = div.getText().trim();
                        if (text.contains("科技有限公司") || text.contains("有限公司") || text.contains("公司")) {
                            if (!text.equals("联系方式") && text.length() > 5) {
                                info.setCompanyName(text);
                                System.out.println("🏢 备用方法提取到公司名称: " + text);
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        // 忽略单个元素的错误
                    }
                }
            } catch (Exception e2) {
                System.err.println("❌ 备用方法也失败了: " + e2.getMessage());
            }
        }

        try {
            // 根据八爪鱼任务，使用更精确的XPath选择器提取联系人
            System.out.println("🔍 尝试查找联系人元素...");
            // 使用八爪鱼任务中的XPath：//div[contains(@style, 'font-size: 16px') and contains(@style, 'color: rgb(18, 18, 18)')]
            WebElement contactNameElement = driver.findElement(By.xpath("//div[contains(@style, 'font-size: 16px') and contains(@style, 'color: rgb(18, 18, 18)')]"));
            String contactName = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent || arguments[0].innerText;", contactNameElement);
            contactName = contactName.trim();
            if (!contactName.isEmpty()) {
                info.setContactPerson(contactName);
                System.out.println("👤 提取到联系人: " + contactName);
            }
        } catch (Exception e) {
            try {
                // 备用选择器 - 查找包含"先生"或"女士"的文本
                System.out.println("🔄 尝试备用方法查找联系人...");
                WebElement contactNameElement = driver.findElement(By.xpath("//div[contains(text(), '先生') or contains(text(), '女士')]"));
                String contactName = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", contactNameElement);
                contactName = contactName.trim();
                if (!contactName.isEmpty()) {
                    info.setContactPerson(contactName);
                    System.out.println("👤 提取到联系人(备用): " + contactName);
                }
            } catch (Exception e2) {
                info.setContactPerson("未获取到联系人");
                System.err.println("❌ 提取联系人失败");
            }
        }

        try {
            // 根据八爪鱼任务，使用更精确的XPath选择器提取电话
            System.out.println("🔍 尝试查找电话元素...");
            // 使用八爪鱼任务中的XPath：//div[contains(text(), '电话：')]/following-sibling::div[1]
            WebElement phoneElement = driver.findElement(By.xpath("//div[contains(text(), '电话：')]/following-sibling::div[1]"));
            String phone = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent || arguments[0].innerText;", phoneElement);
            phone = phone.trim();
            if (!phone.isEmpty() && !phone.equals("暂无")) {
                info.setPhoneNumber(phone);
                System.out.println("📞 提取到电话: " + phone);
            }
        } catch (Exception e) {
            System.err.println("❌ 提取电话失败: " + e.getMessage());
            // 尝试备用方法
            try {
                System.out.println("🔄 尝试备用方法查找电话...");
                List<WebElement> phoneElements = driver.findElements(By.xpath("//*[contains(text(), '电话')]"));
                for (WebElement element : phoneElements) {
                    try {
                        String text = element.getText().trim();
                        if (text.contains("电话") && text.length() > 5) {
                            // 提取电话号码
                            String phoneNumber = extractPhoneNumber(text);
                            if (!phoneNumber.isEmpty()) {
                                info.setPhoneNumber(phoneNumber);
                                System.out.println("📞 备用方法提取到电话: " + phoneNumber);
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        // 忽略单个元素的错误
                    }
                }
            } catch (Exception e2) {
                System.err.println("❌ 备用方法也失败了: " + e2.getMessage());
            }
            if (info.getPhoneNumber() == null || info.getPhoneNumber().equals("未获取到联系电话")) {
                info.setPhoneNumber("未获取到联系电话");
            }
        }

        try {
            // 根据八爪鱼任务，使用更精确的XPath选择器提取手机
            System.out.println("🔍 尝试查找手机元素...");
            // 使用八爪鱼任务中的XPath：//div[contains(text(), '手机：')]/following-sibling::div[1]
            WebElement mobileElement = driver.findElement(By.xpath("//div[contains(text(), '手机：')]/following-sibling::div[1]"));
            String mobile = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent || arguments[0].innerText;", mobileElement);
            mobile = mobile.trim();
            if (!mobile.isEmpty() && !mobile.equals("暂无")) {
                // 如果手机号不为空，优先使用手机号
                info.setPhoneNumber(mobile);
                System.out.println("📱 提取到手机: " + mobile);
            }
        } catch (Exception e) {
            System.err.println("❌ 提取手机号失败: " + e.getMessage());
            // 尝试备用方法
            try {
                System.out.println("🔄 尝试备用方法查找手机...");
                List<WebElement> mobileElements = driver.findElements(By.xpath("//*[contains(text(), '手机')]"));
                for (WebElement element : mobileElements) {
                    try {
                        String text = element.getText().trim();
                        if (text.contains("手机") && text.length() > 5) {
                            // 提取手机号码
                            String mobileNumber = extractPhoneNumber(text);
                            if (!mobileNumber.isEmpty()) {
                                info.setPhoneNumber(mobileNumber);
                                System.out.println("📱 备用方法提取到手机: " + mobileNumber);
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        // 忽略单个元素的错误
                    }
                }
            } catch (Exception e2) {
                System.err.println("❌ 备用方法也失败了: " + e2.getMessage());
            }
        }

        try {
            // 根据八爪鱼任务，使用更精确的XPath选择器提取地址
            System.out.println("🔍 尝试查找地址元素...");
            // 使用八爪鱼任务中的XPath：//div[contains(text(), '地址：')]/following-sibling::div[1]
            WebElement addressElement = driver.findElement(By.xpath("//div[contains(text(), '地址：')]/following-sibling::div[1]"));
            String address = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent || arguments[0].innerText;", addressElement);
            address = address.trim();
            if (!address.isEmpty() && !address.equals("暂无")) {
                info.setAddress(address);
                System.out.println("📍 提取到地址: " + address);
            }
        } catch (Exception e) {
            System.err.println("❌ 提取地址失败: " + e.getMessage());
            // 尝试备用方法
            try {
                System.out.println("🔄 尝试备用方法查找地址...");
                List<WebElement> addressElements = driver.findElements(By.xpath("//*[contains(text(), '地址')]"));
                for (WebElement element : addressElements) {
                    try {
                        String text = element.getText().trim();
                        if (text.contains("地址") && text.length() > 10) {
                            // 提取地址信息
                            String address = extractAddress(text);
                            if (!address.isEmpty()) {
                                info.setAddress(address);
                                System.out.println("📍 备用方法提取到地址: " + address);
                                break;
                            }
                        }
                    } catch (Exception ex) {
                        // 忽略单个元素的错误
                    }
                }
            } catch (Exception e2) {
                System.err.println("❌ 备用方法也失败了: " + e2.getMessage());
            }
            if (info.getAddress() == null || info.getAddress().equals("未获取到地址")) {
                info.setAddress("未获取到地址");
            }
        }

        try {
            // 根据八爪鱼任务，使用更精确的XPath选择器提取传真
            System.out.println("🔍 尝试查找传真元素...");
            // 使用八爪鱼任务中的XPath：//div[contains(text(), '传真：')]/following-sibling::div[1]
            WebElement faxElement = driver.findElement(By.xpath("//div[contains(text(), '传真：')]/following-sibling::div[1]"));
            String fax = (String) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].textContent || arguments[0].innerText;", faxElement);
            fax = fax.trim();
            if (!fax.isEmpty() && !fax.equals("暂无")) {
                info.setBusinessType("传真: " + fax);
                System.out.println("📠 提取到传真: " + fax);
            } else {
                info.setBusinessType("未获取到经营模式");
            }
        } catch (Exception e) {
            System.err.println("❌ 提取传真失败: " + e.getMessage());
            info.setBusinessType("未获取到经营模式");
        }

        try {
            // 主营产品 - 从商品标题推断
            if (info.getProductTitle() != null && !info.getProductTitle().equals("未获取到商品标题")) {
                info.setMainProducts(info.getProductTitle());
            } else {
                info.setMainProducts("未获取到主营产品");
            }
        } catch (Exception e) {
            info.setMainProducts("未获取到主营产品");
        }

        // 综合联系方式信息
        StringBuilder contactInfoBuilder = new StringBuilder();
        if (info.getContactPerson() != null && !info.getContactPerson().equals("未获取到联系人")) {
            contactInfoBuilder.append("联系人: ").append(info.getContactPerson());
        }
        if (info.getPhoneNumber() != null && !info.getPhoneNumber().equals("未获取到联系电话")) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("电话: ").append(info.getPhoneNumber());
        }
        if (info.getAddress() != null && !info.getAddress().equals("未获取到地址")) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("地址: ").append(info.getAddress());
        }
        
        if (contactInfoBuilder.length() > 0) {
            info.setContactInfo(contactInfoBuilder.toString());
        } else {
            info.setContactInfo("未获取到联系方式");
        }
        
        System.out.println("📋 综合联系方式: " + info.getContactInfo());
    }
    
    // 辅助方法：从文本中提取电话号码
    private String extractPhoneNumber(String text) {
        // 移除常见的非数字字符，保留数字、空格、+、-、.
        String cleaned = text.replaceAll("[^0-9\\s\\+\\-\\.]", "");
        // 查找连续的数字序列
        String[] parts = cleaned.split("\\s+");
        for (String part : parts) {
            if (part.matches(".*\\d{7,}.*")) { // 至少7位数字
                return part.trim();
            }
        }
        return "";
    }
    
    // 辅助方法：从文本中提取地址
    private String extractAddress(String text) {
        // 查找包含"地址："的文本，提取后面的内容
        if (text.contains("地址：")) {
            String[] parts = text.split("地址：");
            if (parts.length > 1) {
                return parts[1].trim();
            }
        }
        // 如果没有"地址："，尝试查找包含省市的文本
        if (text.contains("省") || text.contains("市") || text.contains("区") || text.contains("县")) {
            return text.trim();
        }
        return "";
    }

    private void scrollPage(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long pageHeight = (Long) js.executeScript("return document.body.scrollHeight");
        for (int i = 0; i < pageHeight; i += 300) {
            js.executeScript("window.scrollTo(0, " + i + ")");
            antiDetectionService.randomWait(100, 300);
        }
        // 滚动到底部
        js.executeScript("window.scrollTo(0, document.body.scrollHeight)");
        antiDetectionService.randomWait(1000, 2000);
    }

    private boolean tryNextPage(WebDriver driver, WebDriverWait wait) {
        try {
            // 使用您提供的XPath查找下一页按钮
            WebElement nextPage = driver.findElement(By.xpath("//BUTTON[contains(@class,'next-btn next-btn-normal next-btn-large next-pagination-item next')]"));
            
            if (nextPage != null && nextPage.isEnabled()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPage);
                antiDetectionService.randomWait(3000, 5000);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("翻页失败: " + e.getMessage());
            return false;
        }
    }
}