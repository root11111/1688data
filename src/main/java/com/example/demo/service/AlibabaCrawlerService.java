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
                        ManufacturerInfo info = extractBasicInfo(item, url);
                        
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

    private ManufacturerInfo extractBasicInfo(WebElement item, String sourceUrl) {
        ManufacturerInfo info = new ManufacturerInfo();
        info.setCrawlTime(LocalDateTime.now());
        info.setSourceUrl(sourceUrl);

        try {
            // 提取商品标题 - 使用您提供的XPath
            WebElement titleElement = item.findElement(By.xpath(".//a[contains(@href, 'dj.1688.com/ci_bb')]"));
            info.setProductTitle(titleElement.getText().trim());
        } catch (Exception e) {
            info.setProductTitle("未获取到商品标题");
        }

        try {
            // 提取价格信息
            WebElement priceElement = item.findElement(By.xpath(".//span[contains(@class, 'price')]"));
            info.setPrice(priceElement.getText().trim());
        } catch (Exception e) {
            info.setPrice("未获取到价格信息");
        }

        // 公司名称将在详情页的联系方式中获取
        info.setCompanyName("待从详情页获取");

        return info;
    }

    private void extractContactInfo(WebDriver driver, ManufacturerInfo info) {
        try {
            // 根据您提供的HTML结构提取联系方式信息
            // 公司名称 - 在联系方式弹窗中的第二个div
            WebElement companyElement = driver.findElement(By.xpath("//div[contains(text(), '联系方式')]/following-sibling::div[1]"));
            if (companyElement != null) {
                String companyName = companyElement.getText().trim();
                if (!companyName.isEmpty() && !companyName.equals("联系方式")) {
                    info.setCompanyName(companyName);
                }
            }
        } catch (Exception e) {
            // 忽略错误，使用基本信息中的公司名称
        }

        try {
            // 联系人姓名 - 根据HTML结构，在底部的联系人信息区域
            WebElement contactNameElement = driver.findElement(By.xpath("//div[contains(@style, 'font-size: 16px') and contains(@style, 'color: rgb(18, 18, 18)')]"));
            info.setContactPerson(contactNameElement.getText().trim());
        } catch (Exception e) {
            try {
                // 备用选择器
                WebElement contactNameElement = driver.findElement(By.xpath("//div[contains(@style, 'font-size: 16px')]"));
                info.setContactPerson(contactNameElement.getText().trim());
            } catch (Exception e2) {
                info.setContactPerson("未获取到联系人");
            }
        }

        try {
            // 电话 - 根据HTML结构，查找包含"电话："的div后面的div
            WebElement phoneElement = driver.findElement(By.xpath("//div[contains(text(), '电话：')]/following-sibling::div[1]"));
            String phone = phoneElement.getText().trim();
            if (!phone.isEmpty() && !phone.equals("暂无")) {
                info.setPhoneNumber(phone);
            }
        } catch (Exception e) {
            info.setPhoneNumber("未获取到联系电话");
        }

        try {
            // 手机 - 根据HTML结构，查找包含"手机："的div后面的div
            WebElement mobileElement = driver.findElement(By.xpath("//div[contains(text(), '手机：')]/following-sibling::div[1]"));
            String mobile = mobileElement.getText().trim();
            if (!mobile.isEmpty() && !mobile.equals("暂无")) {
                info.setPhoneNumber(mobile); // 优先使用手机号
            }
        } catch (Exception e) {
            // 如果手机号获取失败，保持原来的电话
        }

        try {
            // 地址 - 根据HTML结构，查找包含"地址："的div后面的div
            WebElement addressElement = driver.findElement(By.xpath("//div[contains(text(), '地址：')]/following-sibling::div[1]"));
            String address = addressElement.getText().trim();
            if (!address.isEmpty() && !address.equals("暂无")) {
                info.setAddress(address);
            }
        } catch (Exception e) {
            info.setAddress("未获取到地址");
        }

        try {
            // 传真 - 根据HTML结构，查找包含"传真："的div后面的div
            WebElement faxElement = driver.findElement(By.xpath("//div[contains(text(), '传真：')]/following-sibling::div[1]"));
            String fax = faxElement.getText().trim();
            if (!fax.isEmpty() && !fax.equals("暂无")) {
                info.setBusinessType("传真: " + fax);
            } else {
                info.setBusinessType("未获取到经营模式");
            }
        } catch (Exception e) {
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