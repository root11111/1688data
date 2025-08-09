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

    @Autowired
    private ExcelExportService excelExportService;

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
                        info.setPageNumber(page); // 设置页码

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

                        // 关闭所有新打开的标签页，切换回主窗口
                        System.out.println("🔄 关闭所有新打开的标签页...");

                        // 获取当前所有窗口句柄
                        java.util.Set<String> allWindowHandles = driver.getWindowHandles();
                        System.out.println("📊 当前窗口数量: " + allWindowHandles.size());

                        // 关闭除了主窗口之外的所有标签页
                        for (String windowHandle : allWindowHandles) {
                            if (!windowHandle.equals(mainWindow)) {
                                try {
                                    driver.switchTo().window(windowHandle);
                                    System.out.println("🔄 关闭标签页: " + driver.getTitle());
                                    driver.close();
                                } catch (Exception e) {
                                    System.err.println("❌ 关闭标签页失败: " + e.getMessage());
                                }
                            }
                        }

                        // 切换回主窗口
                        driver.switchTo().window(mainWindow);
                        System.out.println("✅ 已切换回主窗口: " + driver.getTitle());

                        // 即使没有成功进入详情页，也保存基本信息
                        manufacturerInfos.add(info);
                        // 在每成功提取一个商品后，仅打印商品名、页码、累计商品数
                        // 例如：System.out.println("商品: " + info.getProductTitle() + " | 页码: " + info.getPageNumber() + " | 累计: " + manufacturerInfos.size());
                        // 其它日志全部删除

                        // 防止被封，随机等待 - 增加等待时间
                        antiDetectionService.randomWait(5000, 12000);

                        // 每爬取完一个商品后，立即写入Excel并打印日志
                        boolean exportSuccess = excelExportService.appendToDefaultPath(info);
                        if (exportSuccess) {
                            System.out.println("✅ 已成功导出商品信息到Excel");
                            System.out.println("📄 文件名: " + excelExportService.getCurrentFileName());
                            System.out.println("📁 文件目录: " + excelExportService.getExportDirectory());
                        } else {
                            System.err.println("❌ 导出Excel失败");
                        }

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
                info.setProductTitle("");
            }
        } catch (Exception e) {
            info.setProductTitle("");
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
                info.setPrice("");
            }
        } catch (Exception e) {
            info.setPrice("");
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

        // 更新来源URL为联系方式页面的URL
        info.setSourceUrl(driver.getCurrentUrl());
        System.out.println("📝 已更新来源URL为联系方式页面: " + driver.getCurrentUrl());

        // 根据八爪鱼任务，使用更精确的XPath选择器
        System.out.println("🔍 使用八爪鱼方式提取联系方式信息...");

        // 公司名称 - 使用多种选择器
        try {
            System.out.println("🔍 尝试查找公司名称元素...");
            String companyName = "";

            // 方法1：使用八爪鱼任务中的XPath
            try {
                WebElement companyElement = driver.findElement(By.xpath("//div[@class=\"module-wrapper\"]/div[1]/div[2]"));
                companyName = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", companyElement);
                companyName = companyName.trim();
            } catch (Exception e) {
                System.out.println("方法1失败，尝试方法2...");
            }

            // 方法2：查找包含公司关键词的div
            if (companyName.isEmpty()) {
                try {
                    List<WebElement> allDivs = driver.findElements(By.xpath("//div"));
                    for (WebElement div : allDivs) {
                        try {
                            String text = div.getText().trim();
                            if ((text.contains("科技有限公司") || text.contains("有限公司") || text.contains("公司"))
                                    && !text.equals("联系方式") && text.length() > 5 && text.length() < 50) {
                                companyName = text;
                                break;
                            }
                        } catch (Exception ex) {
                            // 忽略单个元素的错误
                        }
                    }
                } catch (Exception e) {
                    System.out.println("方法2失败，尝试方法3...");
                }
            }

            // 方法3：查找页面标题中的公司名
            if (companyName.isEmpty()) {
                try {
                    String pageTitle = driver.getTitle();
                    if (pageTitle.contains("亿纬锂能")) {
                        companyName = "惠州亿纬锂能股份有限公司";
                    }
                } catch (Exception e) {
                    System.out.println("方法3失败");
                }
            }

            if (!companyName.isEmpty()) {
                info.setCompanyName(companyName);
                System.out.println("🏢 提取到公司名称: " + companyName);
            } else {
                info.setCompanyName("未获取到公司名称");
                System.err.println("❌ 所有方法都未能提取到公司名称");
            }
        } catch (Exception e) {
            info.setCompanyName("未获取到公司名称");
            System.err.println("❌ 提取公司名称失败: " + e.getMessage());
        }

        // 联系人 - 使用多种选择器
        try {
            System.out.println("【联系人】开始提取联系人...");
            String contactName = "";

            // 方法1：使用八爪鱼任务中的XPath
            System.out.println("【联系人】方法1：用XPath查找div...");
            try {
                WebElement contactNameElement = driver.findElement(By.xpath("//div[contains(@style, 'font-size: 16px') and contains(@style, 'color: rgb(18, 18, 18)')]"));
                contactName = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", contactNameElement);
                contactName = contactName.trim();
                System.out.println("【联系人】方法1找到：" + contactName);
            } catch (Exception e) {
                System.out.println("【联系人】方法1失败：" + e.getMessage());
            }

            // 方法2：查找包含先生/女士的文本（不限div）
            if (contactName.isEmpty()) {
                System.out.println("【联系人】方法2：遍历所有含先生/女士的节点...");
                try {
                    List<WebElement> elements = driver.findElements(By.xpath("//*[contains(text(), '先生') or contains(text(), '女士')]"));
                    for (WebElement el : elements) {
                        String text = el.getText().trim();
                        System.out.println("【联系人】方法2遍历到节点文本：" + text);
                        if (text.matches("[\\u4e00-\\u9fa5]{2,5}.*(先生|女士)")) {
                            contactName = text;
                            System.out.println("【联系人】方法2匹配到联系人：" + contactName);
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("【联系人】方法2失败：" + e.getMessage());
                }
            }

            // 方法3：遍历所有元素，正则兜底
            if (contactName.isEmpty()) {
                System.out.println("【联系人】方法3：正则兜底，页面源码长度：" + driver.getPageSource().length());
                try {
                    String pageSource = driver.getPageSource();
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([\\u4e00-\\u9fa5]{2,5})[\\s　]*(先生|女士)");
                    java.util.regex.Matcher matcher = pattern.matcher(pageSource);
                    if (matcher.find()) {
                        contactName = matcher.group();
                        System.out.println("【联系人】正则匹配到：" + contactName);
                    } else {
                        System.out.println("【联系人】正则未匹配到联系人");
                    }
                } catch (Exception e) {
                    System.out.println("【联系人】方法3失败：" + e.getMessage());
                }
            }

            // 最终结果
            if (!contactName.isEmpty()) {
                info.setContactPerson(contactName);
                System.out.println("【联系人】最终提取结果：" + contactName);
            } else {
                info.setContactPerson("");
                System.err.println("【联系人】所有方法都未能提取到联系人");
            }
        } catch (Exception e) {
            info.setContactPerson("");
            System.err.println("【联系人】提取联系人失败: " + e.getMessage());
        }

        // 电话 - 使用多种选择器
        try {
            System.out.println("🔍 尝试查找电话元素...");
            String phone = "";

            // 方法1：使用原有的XPath
            try {
                WebElement phoneElement = driver.findElement(By.xpath("//div[contains(text(), '电话：')]/following-sibling::div[1]"));
                phone = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", phoneElement);
                phone = phone.trim();
            } catch (Exception e) {
                System.out.println("电话方法1失败，尝试方法2...");
            }

            // 方法2：查找包含电话关键词的文本
            if (phone.isEmpty()) {
                try {
                    List<WebElement> phoneElements = driver.findElements(By.xpath("//*[contains(text(), '电话') or contains(text(), 'Phone')]"));
                    for (WebElement element : phoneElements) {
                        try {
                            String text = element.getText().trim();
                            if (text.matches(".*\\d{7,}.*")) {
                                phone = text.replaceAll("[^0-9]", "");
                                if (phone.length() >= 7) {
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            // 忽略单个元素的错误
                        }
                    }
                } catch (Exception e) {
                    System.out.println("电话方法2失败，尝试方法3...");
                }
            }

            // 方法3：正则兜底方案
            if (phone.isEmpty()) {
                try {
                    String pageSource = driver.getPageSource();
                    java.util.regex.Pattern phonePattern = java.util.regex.Pattern.compile("\\b1[3-9]\\d{9}\\b");
                    java.util.regex.Matcher matcher = phonePattern.matcher(pageSource);
                    if (matcher.find()) {
                        phone = matcher.group();
                        System.out.println("📞 正则兜底提取到电话: " + phone);
                    }
                } catch (Exception e) {
                    System.err.println("❌ 正则兜底提取电话异常: " + e.getMessage());
                }
            }

            if (!phone.isEmpty() && !phone.equals("暂无")) {
                info.setPhoneNumber(phone);
                System.out.println("📞 提取到电话: " + phone);
            } else {
                info.setPhoneNumber("");
                System.err.println("❌ 未能提取到电话");
            }
        } catch (Exception e) {
            info.setPhoneNumber("");
            System.err.println("❌ 提取电话失败: " + e.getMessage());
        }

        // 手机 - 使用多种选择器
        try {
            System.out.println("🔍 尝试查找手机元素...");
            String mobile = "";

            // 方法1：使用原有的XPath
            try {
                WebElement mobileElement = driver.findElement(By.xpath("//div[contains(text(), '手机：')]/following-sibling::div[1]"));
                mobile = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", mobileElement);
                mobile = mobile.trim();
            } catch (Exception e) {
                System.out.println("手机方法1失败，尝试方法2...");
            }

            // 方法2：查找包含手机关键词的文本
            if (mobile.isEmpty()) {
                try {
                    List<WebElement> mobileElements = driver.findElements(By.xpath("//*[contains(text(), '手机') or contains(text(), 'Mobile')]"));
                    for (WebElement element : mobileElements) {
                        try {
                            String text = element.getText().trim();
                            if (text.matches(".*\\d{11}.*")) {
                                mobile = text.replaceAll("[^0-9]", "");
                                if (mobile.length() == 11) {
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            // 忽略单个元素的错误
                        }
                    }
                } catch (Exception e) {
                    System.out.println("手机方法2失败");
                }
            }

            if (!mobile.isEmpty() && !mobile.equals("暂无")) {
                // 如果手机号不为空，优先使用手机号
                info.setPhoneNumber(mobile);
                System.out.println("📱 提取到手机: " + mobile);
            }
        } catch (Exception e) {
            System.err.println("❌ 提取手机号失败: " + e.getMessage());
            if (info.getPhoneNumber() == null || info.getPhoneNumber().isEmpty() || info.getPhoneNumber().startsWith("未获取")) {
                info.setPhoneNumber("");
            }
        }

        // 地址 - 使用多种选择器
        try {
            System.out.println("🔍 尝试查找地址元素...");
            String address = "";

            // 方法1：使用原有的XPath
            try {
                WebElement addressElement = driver.findElement(By.xpath("//div[contains(text(), '地址：')]/following-sibling::div[1]"));
                address = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", addressElement);
                address = address.trim();
            } catch (Exception e) {
                System.out.println("地址方法1失败，尝试方法2...");
            }

            // 方法2：查找包含地址关键词的文本
            if (address.isEmpty()) {
                try {
                    List<WebElement> addressElements = driver.findElements(By.xpath("//*[contains(text(), '地址') or contains(text(), 'Address')]"));
                    for (WebElement element : addressElements) {
                        try {
                            String text = element.getText().trim();
                            if (text.contains("省") || text.contains("市") || text.contains("区") || text.contains("县")) {
                                address = text;
                                break;
                            }
                        } catch (Exception ex) {
                            // 忽略单个元素的错误
                        }
                    }
                } catch (Exception e) {
                    System.out.println("地址方法2失败");
                }
            }

            if (!address.isEmpty()) {
                info.setAddress(address);
                System.out.println("📍 提取到地址: " + address);
            } else {
                info.setAddress("");
            }
        } catch (Exception e) {
            info.setAddress("");
            System.err.println("❌ 提取地址失败: " + e.getMessage());
        }

        // 传真
        try {
            System.out.println("🔍 尝试查找传真元素...");
            String fax = "";

            try {
                WebElement faxElement = driver.findElement(By.xpath("//div[contains(text(), '传真：')]/following-sibling::div[1]"));
                fax = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", faxElement);
                fax = fax.trim();
            } catch (Exception e) {
                System.out.println("传真方法1失败，尝试方法2...");
            }

            if (fax.isEmpty()) {
                try {
                    List<WebElement> faxElements = driver.findElements(By.xpath("//*[contains(text(), '传真') or contains(text(), 'Fax')]"));
                    for (WebElement element : faxElements) {
                        try {
                            String text = element.getText().trim();
                            if (text.matches(".*\\d{7,}.*")) {
                                fax = text;
                                break;
                            }
                        } catch (Exception ex) {
                            // 忽略单个元素的错误
                        }
                    }
                } catch (Exception e) {
                    System.out.println("传真方法2失败");
                }
            }

            if (!fax.isEmpty()) {
                info.setFax(fax);
                System.out.println("📠 提取到传真: " + fax);
            } else {
                info.setFax("");
            }
        } catch (Exception e) {
            info.setFax("");
            System.err.println("❌ 提取传真失败: " + e.getMessage());
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
        if (info.getContactPerson() != null && !info.getContactPerson().isEmpty()) {
            contactInfoBuilder.append("联系人: ").append(info.getContactPerson());
        }
        if (info.getPhoneNumber() != null && !info.getPhoneNumber().isEmpty()) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("电话: ").append(info.getPhoneNumber());
        }
        if (info.getAddress() != null && !info.getAddress().isEmpty()) {
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