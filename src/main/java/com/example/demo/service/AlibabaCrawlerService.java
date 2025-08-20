package com.example.demo.service;

import com.example.demo.entity.ManufacturerInfo;
import com.example.demo.entity.CrawlProgress;
import com.example.demo.service.CrawlProgressService;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AlibabaCrawlerService {

    @Autowired
    private CaptchaHandlerService captchaHandler;

    @Autowired
    private AntiDetectionService antiDetectionService;

    @Autowired
    private ManufacturerInfoService manufacturerInfoService;
    
    @Autowired
    private CrawlProgressService crawlProgressService;
    
    @Autowired
    private CrawlTaskService crawlTaskService;


    /**
     * 爬取供应商信息（支持断点续传）
     */
    public List<ManufacturerInfo> crawlManufacturerInfo(String url, int maxPages, Long taskId) {
        // 检查是否有未完成的爬取任务
        Optional<CrawlProgress> existingProgress = crawlProgressService.findByUrl(url);
        CrawlProgress progress;
        
        // 优先使用任务表中的进度信息（更准确）
        Integer startPage = 1;
        Integer startItemIndex = 0;
        
        if (taskId != null) {
            try {
                var task = crawlTaskService.getTaskById(taskId);
                if (task.isPresent() && task.get().getCurrentPage() != null && task.get().getCurrentPage() > 0) {
                    startPage = task.get().getCurrentPage();
                    startItemIndex = task.get().getCurrentItemIndex() != null ? task.get().getCurrentItemIndex() : 0;
                    System.out.println("🔄 从任务表获取进度信息: 第" + startPage + "页，第" + startItemIndex + "项");
                }
            } catch (Exception e) {
                System.err.println("⚠️ 获取任务进度信息失败: " + e.getMessage());
            }
        }
        
        if (existingProgress.isPresent()) {
            progress = existingProgress.get();
            // 如果任务表有更准确的进度信息，使用任务表的
            if (startPage > 1 || startItemIndex > 0) {
                progress.setCurrentPage(startPage);
                progress.setCurrentItemIndex(startItemIndex);
                System.out.println("🔄 发现未完成的爬取任务，使用任务表进度从断点继续...");
            } else {
                System.out.println("🔄 发现未完成的爬取任务，使用进度表从断点继续...");
            }
            System.out.println("📊 当前进度: 第" + progress.getCurrentPage() + "页，第" + progress.getCurrentItemIndex() + "项");
            System.out.println("📊 任务状态: " + progress.getStatus());
        } else {
            // 创建新的爬取进度记录
            progress = crawlProgressService.createProgress(url, maxPages, taskId);
            // 设置起始进度
            progress.setCurrentPage(startPage);
            progress.setCurrentItemIndex(startItemIndex);
            System.out.println("🆕 创建新的爬取任务，从第" + startPage + "页开始...");
        }
        
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
            System.out.println("🔄 正在访问页面: " + url);
            driver.get(url);

            // 执行反检测脚本
            System.out.println("🔧 执行反检测脚本...");
            antiDetectionService.executeAntiDetectionScripts(driver);

            // 等待页面加载
            System.out.println("⏳ 等待页面加载...");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));

            // 模拟人类行为
            System.out.println("🤖 模拟人类行为...");
            antiDetectionService.simulateHumanBehavior(driver);

            // 随机等待，模拟人类行为
            System.out.println("⏰ 随机等待...");
            antiDetectionService.randomWait(2000, 5000);

            // 2. 循环翻页（支持断点续传）
            System.out.println("🔄 断点续传信息:");
            System.out.println("   - 起始页: " + progress.getCurrentPage());
            System.out.println("   - 起始项索引: " + progress.getCurrentItemIndex());
            System.out.println("   - 最大页数: " + maxPages);
            System.out.println("   - 任务ID: " + taskId);
            
            for (int page = progress.getCurrentPage(); page <= maxPages; page++) {
                System.out.println("📄 ========== 开始处理第 " + page + " 页 ==========");
                
                // 如果要从第2页或之后的页面开始，需要先翻页到指定页面
                if (page > 1) {
                    System.out.println("🔄 需要从第" + page + "页开始，先翻页到指定页面...");
                    System.out.println("🔄 当前浏览器实际在第1页，需要翻页到第" + page + "页");
                    
                    if (!navigateToPage(driver, wait, page)) {
                        System.err.println("❌ 无法翻页到第" + page + "页，爬取终止");
                        break;
                    }
                    System.out.println("✅ 已成功翻页到第" + page + "页");
                }
                
                // 更新进度状态
                progress.setStatus("IN_PROGRESS");
                progress.setCurrentPage(page);
                crawlProgressService.updateProgress(progress.getId(), page, progress.getCurrentItemIndex(), "IN_PROGRESS");
                
                // 同步更新任务表的进度信息
                try {
                    if (progress.getTaskId() != null) {
                        crawlTaskService.updateTaskProgress(progress.getTaskId(), page, progress.getCurrentItemIndex());
                    } else {
                        System.out.println("📄 第" + page + "页 - ⚠️ 进度记录没有关联任务ID，跳过任务表更新");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ 更新任务表进度失败: " + e.getMessage());
                }

                // 3. 滚动网页
                System.out.println("📄 第" + page + "页 - 开始滚动页面...");
                scrollPage(driver);

                // 4. 获取商品列表 - 使用八爪鱼的方式：不固定元素列表，动态获取
                List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                int totalItemsOnPage = items.size(); // 保存原始商品数量
                int processedItemsOnPage = 0; // 实际处理的商品数量
                System.out.println("📄 第" + page + "页 - 找到 " + totalItemsOnPage + " 个商品");
                
                // 如果没有找到商品，尝试其他选择器
                if (totalItemsOnPage == 0) {
                    System.out.println("📄 第" + page + "页 - ⚠️ 使用默认选择器未找到商品，尝试其他选择器...");
                    items = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
                    totalItemsOnPage = items.size();
                    System.out.println("📄 第" + page + "页 - 🔍 使用备用选择器找到 " + totalItemsOnPage + " 个商品");
                }

                // 确定开始处理的商品索引（支持断点续传）
                int startIndex = (page == progress.getCurrentPage()) ? progress.getCurrentItemIndex() : 0;
                System.out.println("📄 第" + page + "页 - 🔍 开始处理，起始索引: " + startIndex + "，商品总数: " + items.size());
                
                for (int i = startIndex; i < items.size(); i++) {
                    try {
                        // 更新当前处理的商品索引
                        progress.setCurrentItemIndex(i);
                        crawlProgressService.updateProgress(progress.getId(), page, i, "IN_PROGRESS");
                        
                        // 同步更新任务表的进度信息
                        try {
                            if (progress.getTaskId() != null) {
                                crawlTaskService.updateTaskProgress(progress.getTaskId(), page, i);
                            } else {
                                System.out.println("📄 第" + page + "页 - ⚠️ 进度记录没有关联任务ID，跳过任务表更新");
                            }
                        } catch (Exception e) {
                            System.err.println("⚠️ 更新任务表进度失败: " + e.getMessage());
                        }
                        
                        System.out.println("📄 第" + page + "页 - 🎯 开始处理第 " + (i + 1) + " 个商品...");
                        
                        // 验证WebDriver会话是否仍然有效
                        try {
                            driver.getTitle(); // 简单的会话验证
                        } catch (Exception sessionEx) {
                            System.err.println("📄 第" + page + "页 - ❌ WebDriver会话已失效: " + sessionEx.getMessage());
                            System.err.println("📄 第" + page + "页 - ❌ 爬取终止，请重启程序");
                            // 保存当前进度
                            crawlProgressService.updateProgress(progress.getId(), page, i, "FAILED");
                            return manufacturerInfos; // 直接返回已获取的数据
                        }

                        // 重新获取元素列表，防止StaleElementReferenceException
                        items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                        System.out.println("📄 第" + page + "页 - 🔍 重新获取商品列表，当前找到 " + items.size() + " 个商品，正在处理第 " + (i + 1) + " 个");
                        if (i >= items.size()) {
                            System.out.println("📄 第" + page + "页 - ⚠️ 商品索引超出范围，结束当前页面处理");
                            break;
                        }

                        WebElement item = items.get(i);

                        // 提取商品基本信息
                        ManufacturerInfo info = extractBasicInfo(item, url, driver);
                        info.setPageNumber(page); // 设置页码
                        
                        // 先保存基本信息到数据库，获取ID
                        ManufacturerInfo savedInfo = null;
                        try {
                            savedInfo = manufacturerInfoService.save(info);
                            if (savedInfo != null && savedInfo.getId() != null) {
                                System.out.println("📄 第" + page + "页 - ✅ 基本信息已保存到数据库: " + info.getCompanyName() + " (ID: " + savedInfo.getId() + ")");
                                // 更新info对象，确保有正确的ID
                                info.setId(savedInfo.getId());
                            } else {
                                System.err.println("📄 第" + page + "页 - ❌ 基本信息保存到数据库失败: " + info.getCompanyName());
                                continue; // 如果保存失败，跳过此商品
                            }
                        } catch (Exception dbEx) {
                            System.err.println("📄 第" + page + "页 - ❌ 保存基本信息异常: " + dbEx.getMessage());
                            dbEx.printStackTrace();
                            continue; // 如果保存失败，跳过此商品
                        }

                        // 5. 点击列表链接进入详情页
                        String mainWindow = driver.getWindowHandle();
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", item);
                        antiDetectionService.randomWait(1000, 2000);

                        // 按照八爪鱼的方式：点击列表链接
                        System.out.println("📄 第" + page + "页 - 🖱️ 尝试点击第 " + (i + 1) + " 个商品链接...");

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

                            System.out.println("📄 第" + page + "页 - ✅ 成功打开商品详情页");
                        } else {
                            System.out.println("📄 第" + page + "页 - ⚠️ 点击链接未打开新页面，尝试获取链接直接打开");
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
                                System.err.println("📄 第" + page + "页 - ❌ 无法获取商品链接，跳过此商品");
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

                        // 再次检查联系方式页面是否出现验证码
                        if (captchaHandler.checkForCaptcha(driver)) {
                            System.out.println("📄 第" + page + "页 - ⚠️ 检测到验证码，尝试处理...");
                            if (captchaHandler.handleCaptcha(driver)) {
                                System.out.println("📄 第" + page + "页 - ✅ 验证码处理成功，继续...");
                            } else {
                                System.out.println("📄 第" + page + "页 - ❌ 验证码处理失败，跳过此商品");
                                continue;
                            }
                        }

                        // 等待新页面加载并提取详细信息
                        try {
                            System.out.println("📄 第" + page + "页 - 🔍 等待联系方式按钮出现...");
                            // 等待联系方式按钮出现
                            WebElement contactButton = wait.until(ExpectedConditions.elementToBeClickable(
                                    By.xpath("//a[contains(text(), '联系方式')]")));

                            // 6. 点击联系方式按钮
                            System.out.println("📄 第" + page + "页 - 🖱️ 点击联系方式按钮...");
                            String currentWindow = driver.getWindowHandle();
                            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", contactButton);
                            antiDetectionService.randomWait(2000, 4000);


                            // 切换到联系方式新页面
                            for (String windowHandle : driver.getWindowHandles()) {
                                if (!windowHandle.equals(currentWindow)) {
                                   driver.switchTo().window(windowHandle);

                                    // 等待新页面加载
                                    antiDetectionService.randomWait(2000, 3000);

                                    String newPageUrl = driver.getCurrentUrl();
                                    String newPageTitle = driver.getTitle();

                                    System.out.println("📄 第" + page + "页 - 🔍 检查新页面 - URL: " + newPageUrl);
                                    System.out.println("📄 第" + page + "页 - 🔍 检查新页面 - Title: " + newPageTitle);


                                }
                            }


                            // 再次检查联系方式页面是否出现验证码
                            if (captchaHandler.checkForCaptcha(driver)) {
                                System.out.println("📄 第" + page + "页 - ⚠️ 联系方式页面检测到验证码，尝试处理...");
                                if (captchaHandler.handleCaptcha(driver)) {
                                    System.out.println("📄 第" + page + "页 - ✅ 联系方式页面验证码处理成功");
                                } else {
                                    System.out.println("📄 第" + page + "页 - ❌ 联系方式页面验证码处理失败，跳过此商品");
                                    continue;
                                }
                            }


                            // 7. 在联系方式页面提取数据
                            System.out.println("📄 第" + page + "页 - 📋 开始提取联系方式信息...");
                            extractContactInfo(driver, info);

                            // 提取完联系方式信息后，更新数据库
                            try {
                                ManufacturerInfo updatedInfo = manufacturerInfoService.save(info);
                                if (updatedInfo != null) {
                                    System.out.println("📄 第" + page + "页 - ✅ 联系方式信息已更新到数据库: " + info.getCompanyName());
                                    // 添加到内存列表（用于返回）
                                    manufacturerInfos.add(updatedInfo);
                                    // 增加成功处理计数
                                    processedItemsOnPage++;
                                    
                                                                         // 更新当前商品索引到任务表
                                     try {
                                         if (progress.getTaskId() != null) {
                                             int currentItemIndex = i; // 当前处理的商品索引（从0开始，与循环索引保持一致）
                                             crawlTaskService.updateTaskProgress(progress.getTaskId(), page + 1, currentItemIndex);
                                             System.out.println("📄 第" + page + "页 - 📊 已更新任务进度: 第" + (page + 1) + "页，第" + (currentItemIndex + 1) + "项");
                                         } else {
                                             System.out.println("📄 第" + page + "页 - ⚠️ 进度记录没有关联任务ID，跳过任务表更新");
                                         }
                                     } catch (Exception progressEx) {
                                         System.err.println("⚠️ 更新任务进度失败: " + progressEx.getMessage());
                                     }
                                } else {
                                    System.err.println("📄 第" + page + "页 - ❌ 联系方式信息更新到数据库失败: " + info.getCompanyName());
                                }
                            } catch (Exception updateEx) {
                                System.err.println("📄 第" + page + "页 - ❌ 更新联系方式信息异常: " + updateEx.getMessage());
                                updateEx.printStackTrace();
                            }

                            // 提取完成后关闭联系方式页面，切换回详情页
                            driver.close();
                            driver.switchTo().window(currentWindow);
                            System.out.println("📄 第" + page + "页 - 🔄 已关闭联系方式页面，切换回详情页");


                        } catch (Exception e) {
                            System.err.println("📄 第" + page + "页 - ❌ 提取详细信息失败: " + e.getMessage());
                        }

                        // 安全地清理剩余的新标签页，确保主窗口不被关闭
                        System.out.println("📄 第" + page + "页 - 🔄 安全清理剩余的新标签页...");

                        try {
                            // 获取当前所有窗口句柄
                            java.util.Set<String> allWindowHandles = driver.getWindowHandles();
                            System.out.println("📄 第" + page + "页 - 📊 当前窗口数量: " + allWindowHandles.size());

                            // 如果窗口数量大于1，说明还有其他窗口需要关闭
                            if (allWindowHandles.size() > 1) {
                                // 先确保当前在主窗口
                                driver.switchTo().window(mainWindow);
                                System.out.println("📄 第" + page + "页 - 🏠 已切换到主窗口");

                                // 重新获取窗口句柄列表
                                allWindowHandles = driver.getWindowHandles();

                                // 关闭除了主窗口之外的所有剩余标签页
                                for (String windowHandle : allWindowHandles) {
                                    if (!windowHandle.equals(mainWindow)) {
                                        try {
                                            driver.switchTo().window(windowHandle);
                                            String pageTitle = "";
                                            try {
                                                pageTitle = driver.getTitle();
                                            } catch (Exception titleEx) {
                                                pageTitle = "无法获取标题";
                                            }
                                            System.out.println("📄 第" + page + "页 - 🔄 关闭剩余标签页: " + pageTitle);
                                            driver.close();
                                        } catch (Exception e) {
                                            System.err.println("📄 第" + page + "页 - ❌ 关闭剩余标签页失败: " + e.getMessage());
                                        }
                                    }
                                }

                                // 最后确保切换回主窗口
                                driver.switchTo().window(mainWindow);
                                System.out.println("📄 第" + page + "页 - ✅ 已确保切换回主窗口");
                            } else {
                                System.out.println("📄 第" + page + "页 - ✅ 当前只有主窗口，无需清理");
                            }

                            // 验证主窗口是否仍然有效
                            try {
                                String mainTitle = driver.getTitle();
                                System.out.println("📄 第" + page + "页 - ✅ 主窗口验证成功: " + mainTitle);
                            } catch (Exception e) {
                                System.err.println("📄 第" + page + "页 - ❌ 主窗口验证失败: " + e.getMessage());
                                // 不要重新抛出异常，继续处理下一个商品
                                // throw e; // 注释掉这行，防止循环中断
                            }

                        } catch (Exception e) {
                            System.err.println("📄 第" + page + "页 - ❌ 窗口清理过程出错: " + e.getMessage());
                            // 不要重新抛出异常，继续处理下一个商品
                        }

                        // 注意：这里不需要重复添加，因为已经在前面添加过了
                        // manufacturerInfos.add(info); // 注释掉，避免重复添加
                        
                        // 打印商品处理进度
                        System.out.println("📄 第" + page + "页 - ✅ 商品处理完成: " + info.getProductTitle() + " | 页码: " + info.getPageNumber() + " | 累计: " + manufacturerInfos.size());

                        // 防止被封，随机等待 - 增加等待时间
                        antiDetectionService.randomWait(5000, 12000);

                        // 注意：Excel已经在前面写入过了，这里不需要重复写入
                        // 只打印进度信息
                        System.out.println("📄 第" + page + "页 - 📊 当前页面进度: " + processedItemsOnPage + "/" + totalItemsOnPage + " 个商品");

                        // 打印循环进度信息
                        System.out.println("📄 第" + page + "页 - 🔄 完成第 " + (i + 1) + " 个商品，继续处理下一个商品...");
                        System.out.println("📄 第" + page + "页 - 📊 已处理: " + processedItemsOnPage + "/" + totalItemsOnPage + " 个商品，累计数据: " + manufacturerInfos.size() + " 条");

                    } catch (Exception e) {
                        System.err.println("📄 第" + page + "页 - ❌ 处理第 " + (i + 1) + " 个商品时出错: " + e.getMessage());
                        e.printStackTrace(); // 打印完整的异常堆栈，便于调试
                        System.out.println("📄 第" + page + "页 - ⚠️ 跳过第 " + (i + 1) + " 个商品，继续处理下一个...");
                        continue;
                    }
                }

                System.out.println("📄 ========== 第 " + page + " 页处理完成 ==========");
                System.out.println("📄 第" + page + "页 - 📝 共找到 " + totalItemsOnPage + " 个商品，成功处理 " + processedItemsOnPage + " 个");
                
                // 只有当当前页面所有商品都处理完才翻页
                if (processedItemsOnPage > 0) {
                    // 尝试翻页 - 使用您提供的XPath
                    if (page < maxPages) {
                        System.out.println("📄 第" + page + "页 - 🔄 准备翻到第 " + (page + 1) + " 页...");
                        if (!tryNextPage(driver, wait)) {
                            System.out.println("📄 第" + page + "页 - ⚠️ 没有更多页面了");
                            break;
                        }
                        
                        // 翻页成功后，重置商品索引为0，因为新页面从第一个商品开始
                        progress.setCurrentItemIndex(0);
                        progress.setCurrentPage(page + 1);
                        crawlProgressService.updateProgress(progress.getId(), page + 1, 0, "IN_PROGRESS");
                        
                                                 // 同步更新任务表的进度信息
                         try {
                             if (progress.getTaskId() != null) {
                                 crawlTaskService.updateTaskProgress(progress.getTaskId(), page + 1, 0);
                                 System.out.println("📄 第" + (page + 1) + "页 - 📊 已更新任务表进度: 第" + (page + 1) + "页，第0项");
                             } else {
                                 System.out.println("📄 第" + (page + 1) + "页 - ⚠️ 进度记录没有关联任务ID，跳过任务表更新");
                             }
                         } catch (Exception e) {
                             System.err.println("⚠️ 翻页后更新任务表进度失败: " + e.getMessage());
                         }
                        
                        System.out.println("📄 第" + (page + 1) + "页 - 🔄 翻页成功，重置商品索引为0");
                    } else {
                        System.out.println("📄 第" + page + "页 - ✅ 已达到最大页数限制: " + maxPages);
                    }
                } else {
                    System.out.println("📄 第" + page + "页 - ⚠️ 没有成功处理任何商品，跳过翻页");
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("爬取过程中出错: " + e.getMessage());
            e.printStackTrace();
            // 保存异常状态
            if (progress != null) {
                crawlProgressService.updateStatus(progress.getId(), "FAILED");
            }
        } finally {
            // 更新爬取状态
            if (progress != null) {
                if (progress.getCurrentPage() >= maxPages) {
                    crawlProgressService.updateStatus(progress.getId(), "COMPLETED");
                    System.out.println("🎉 爬取任务完成！");
                } else {
                    crawlProgressService.updateStatus(progress.getId(), "IN_PROGRESS");
                    System.out.println("⏸️ 爬取任务暂停，下次可从断点继续");
                }
            }
            driver.quit();
        }

        System.out.println("爬取完成，共获取 " + manufacturerInfos.size() + " 条供应商信息");
        return manufacturerInfos;
    }
    
    /**
     * 继续未完成的爬取任务
     */
    public List<ManufacturerInfo> resumeIncompleteTasks() {
        List<ManufacturerInfo> allResults = new ArrayList<>();
        List<CrawlProgress> incompleteTasks = crawlProgressService.findIncompleteTasks();
        
        if (incompleteTasks.isEmpty()) {
            System.out.println("📋 没有未完成的爬取任务");
            return allResults;
        }
        
        System.out.println("🔄 发现 " + incompleteTasks.size() + " 个未完成的爬取任务，开始继续...");
        
        for (CrawlProgress task : incompleteTasks) {
            if ("FAILED".equals(task.getStatus()) || "IN_PROGRESS".equals(task.getStatus())) {
                System.out.println("🔄 继续任务: " + task.getUrl());
                try {
                    List<ManufacturerInfo> results = crawlManufacturerInfo(task.getUrl(), task.getTotalPages(), task.getTaskId());
                    allResults.addAll(results);
                } catch (Exception e) {
                    System.err.println("❌ 继续任务失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        return allResults;
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
        
        // 设置爬取时间
        info.setCrawlTime(LocalDateTime.now());

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

        // 座机电话 - 使用多种选择器
        try {
            System.out.println("🔍 尝试查找座机电话元素...");
            String landlinePhone = "";

            // 方法1：使用原有的XPath
            try {
                WebElement phoneElement = driver.findElement(By.xpath("//div[contains(text(), '电话：')]/following-sibling::div[1]"));
                landlinePhone = (String) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].textContent || arguments[0].innerText;", phoneElement);
                landlinePhone = landlinePhone.trim();
            } catch (Exception e) {
                System.out.println("座机电话方法1失败，尝试方法2...");
            }

            if (!landlinePhone.isEmpty() && !landlinePhone.equals("暂无")) {
                info.setLandlinePhone(landlinePhone);
                System.out.println("📞 提取到座机电话: " + landlinePhone);
            } else {
                info.setLandlinePhone("");
                System.err.println("❌ 未能提取到座机电话");
            }
        } catch (Exception e) {
            info.setLandlinePhone("");
            System.err.println("❌ 提取座机电话失败: " + e.getMessage());
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


            if (!mobile.isEmpty() && !mobile.equals("暂无")) {
                // 设置手机号到专门的字段
                info.setMobilePhone(mobile);
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
        if (info.getLandlinePhone() != null && !info.getLandlinePhone().isEmpty()) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("座机: ").append(info.getLandlinePhone());
        }
        if (info.getMobilePhone() != null && !info.getMobilePhone().isEmpty()) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("手机: ").append(info.getMobilePhone());
        }
        if (info.getAddress() != null && !info.getAddress().isEmpty()) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("地址: ").append(info.getAddress());
        }
        if (info.getFax() != null && !info.getFax().isEmpty() && !info.getFax().equals("暂无")) {
            if (contactInfoBuilder.length() > 0) {
                contactInfoBuilder.append(" | ");
            }
            contactInfoBuilder.append("传真: ").append(info.getFax());
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

    /**
     * 翻页到下一页
     */
    private boolean tryNextPage(WebDriver driver, WebDriverWait wait) {
        try {
            // 使用您提供的XPath查找下一页按钮
            WebElement nextPage = driver.findElement(By.xpath("//BUTTON[contains(@class,'next-btn next-btn-normal next-btn-large next-pagination-item next')]"));

            if (nextPage != null && nextPage.isEnabled()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPage);
                
                // 等待新页面加载完成
                System.out.println("🔄 等待新页面加载...");
                antiDetectionService.randomWait(3000, 5000);
                
                // 等待新页面的商品列表加载完成
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
                    System.out.println("✅ 新页面商品列表加载完成");
                    
                    // 验证新页面确实有数据
                    List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                    if (items.size() > 0) {
                        System.out.println("✅ 新页面验证成功，找到 " + items.size() + " 个商品");
                    } else {
                        System.out.println("⚠️ 新页面商品列表为空，尝试备用选择器...");
                        items = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
                        if (items.size() > 0) {
                            System.out.println("✅ 备用选择器验证成功，找到 " + items.size() + " 个商品");
                        } else {
                            System.err.println("❌ 新页面商品列表为空，翻页可能失败");
                            return false;
                        }
                    }
                    
                } catch (Exception e) {
                    System.out.println("⚠️ 等待新页面商品列表加载超时，尝试备用选择器...");
                    try {
                        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'offer_item')]")));
                        System.out.println("✅ 新页面商品列表加载完成（备用选择器）");
                        
                        // 验证备用选择器的数据
                        List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
                        if (items.size() > 0) {
                            System.out.println("✅ 备用选择器验证成功，找到 " + items.size() + " 个商品");
                        } else {
                            System.err.println("❌ 备用选择器商品列表也为空，翻页失败");
                            return false;
                        }
                        
                    } catch (Exception e2) {
                        System.err.println("❌ 新页面商品列表加载失败: " + e2.getMessage());
                        return false;
                    }
                }
                
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("翻页失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 翻页到指定页面
     */
    private boolean navigateToPage(WebDriver driver, WebDriverWait wait, int targetPage) {
        try {
            System.out.println("🔄 开始翻页到第" + targetPage + "页...");
            
            // 获取当前页面的页码信息
            int currentPage = getCurrentPageNumber(driver);
            System.out.println("🔄 当前实际在第" + currentPage + "页");
            
            // 计算需要翻页的次数
            int pagesToNavigate = targetPage - currentPage;
            if (pagesToNavigate <= 0) {
                System.out.println("✅ 已经在第" + currentPage + "页，无需翻页");
                return true;
            }
            
            System.out.println("🔄 需要翻页 " + pagesToNavigate + " 次才能到达第" + targetPage + "页");
            
            // 翻页指定次数
            for (int i = 0; i < pagesToNavigate; i++) {
                System.out.println("🔄 第" + (i + 1) + "次翻页，目标：第" + (currentPage + i + 1) + "页...");
                
                if (!tryNextPage(driver, wait)) {
                    System.err.println("❌ 第" + (i + 1) + "次翻页失败，无法到达第" + targetPage + "页");
                    return false;
                }
                
                System.out.println("✅ 第" + (i + 1) + "次翻页成功，当前在第" + (currentPage + i + 1) + "页");
                
                // 翻页后等待一下，确保页面稳定
                antiDetectionService.randomWait(2000, 4000);
            }
            
            System.out.println("🎯 成功翻页到第" + targetPage + "页！");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 翻页到第" + targetPage + "页时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前页面的页码
     */
    private int getCurrentPageNumber(WebDriver driver) {
        try {
            // 尝试从页面元素获取当前页码
            List<WebElement> pageElements = driver.findElements(By.xpath("//button[contains(@class, 'next-pagination-item') and not(contains(@class, 'next'))]"));
            
            for (WebElement element : pageElements) {
                try {
                    String text = element.getText().trim();
                    if (text.matches("\\d+")) {
                        int pageNum = Integer.parseInt(text);
                        System.out.println("🔍 从页面元素检测到当前页码: " + pageNum);
                        return pageNum;
                    }
                } catch (Exception e) {
                    // 忽略单个元素的错误
                }
            }
            
            // 如果无法从页面获取，尝试从URL或其他方式推断
            System.out.println("⚠️ 无法从页面元素获取页码，使用默认值1");
            return 1;
            
        } catch (Exception e) {
            System.err.println("❌ 获取当前页码失败: " + e.getMessage());
            return 1;
        }
    }
}