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
import org.springframework.transaction.annotation.Transactional;

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

            // 🆕 检查主页面是否出现验证码
            if (captchaHandler.checkForCaptcha(driver)) {
                System.out.println("📄 主页面检测到验证码，尝试处理...");
                CaptchaHandlerService.CaptchaResult result = captchaHandler.handleCaptcha(driver);
                if (result == CaptchaHandlerService.CaptchaResult.SUCCESS) {
                    System.out.println("📄 主页面验证码处理成功，继续...");
                } else if (result == CaptchaHandlerService.CaptchaResult.FAILED) {
                    System.err.println("📄 主页面验证码处理失败，需要重新加载页面...");
                    // 🆕 验证码处理失败，重新加载主页面
                    driver.navigate().refresh();
                    antiDetectionService.randomWait(3000, 5000);
                    // 重新执行反检测和人类行为模拟
                    antiDetectionService.executeAntiDetectionScripts(driver);
                    antiDetectionService.simulateHumanBehavior(driver);
                    scrollPage(driver);
                    // 重新检查验证码
                    if (captchaHandler.checkForCaptcha(driver)) {
                        System.err.println("📄 重新加载后仍有验证码，尝试再次处理...");
                        result = captchaHandler.handleCaptcha(driver);
                        if (result == CaptchaHandlerService.CaptchaResult.BLOCKED) {
                            System.err.println("📄 主页面验证码被阻止，爬取终止");
                            return manufacturerInfos;
                        }
                    }
                } else {
                    System.err.println("📄 主页面验证码被阻止，爬取终止");
                    return manufacturerInfos;
                }
            }

            // 模拟人类行为
            System.out.println("🤖 模拟人类行为...");
            antiDetectionService.simulateHumanBehavior(driver);

            // 随机等待，模拟人类行为
            System.out.println("⏰ 随机等待...");
            antiDetectionService.randomWait(2000, 5000);

            // 🆕 修复断点续传逻辑：在开始循环前，先检查是否需要直接导航到断点页面
            System.out.println("🔄 断点续传信息:");
            System.out.println("   - 起始页: " + progress.getCurrentPage());
            System.out.println("   - 起始项索引: " + progress.getCurrentItemIndex());
            System.out.println("   - 最大页数: " + maxPages);
            System.out.println("   - 任务ID: " + taskId);
            System.out.println("🔄 断点续传逻辑:");
            System.out.println("   - 循环将从第" + progress.getCurrentPage() + "页开始");
            
                         // 🆕 修复断点续传逻辑：如果起始页 > 1，需要从第1页逐页点击到目标页
             if (progress.getCurrentPage() > 1) {
                 System.out.println("🔄 断点续传：起始页 > 1，需要从第1页逐页点击到第" + progress.getCurrentPage() + "页");
                 
                 // 从第1页开始，逐页点击到目标页
                 for (int targetPage = 2; targetPage <= progress.getCurrentPage(); targetPage++) {
                     System.out.println("🔄 准备点击到第" + targetPage + "页...");
                     
                     // 等待页面加载完成
                     antiDetectionService.randomWait(2000, 3000);
                     
                     // 查找并点击下一页按钮
                     try {
                         WebElement nextPageButton = driver.findElement(By.xpath("//BUTTON[contains(@class,'next-btn next-btn-normal next-btn-large next-pagination-item next')]"));
                         if (nextPageButton != null && nextPageButton.isEnabled()) {
                             System.out.println("🔄 找到下一页按钮，点击...");
                             ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPageButton);
                             
                             // 等待新页面加载
                             System.out.println("⏳ 等待第" + targetPage + "页加载...");
                             antiDetectionService.randomWait(3000, 5000);
                             
                             // 等待商品列表加载完成
                             try {
                                 wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
                                 System.out.println("✅ 第" + targetPage + "页商品列表加载完成");
                             } catch (Exception e) {
                                 System.out.println("⚠️ 等待第" + targetPage + "页商品列表加载超时，尝试备用选择器...");
                                 try {
                                     wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'offer_item')]")));
                                     System.out.println("✅ 第" + targetPage + "页商品列表加载完成（备用选择器）");
                                 } catch (Exception e2) {
                                     System.err.println("❌ 第" + targetPage + "页商品列表加载失败: " + e2.getMessage());
                                     System.err.println("❌ 无法到达第" + progress.getCurrentPage() + "页，爬取终止");
                                     return manufacturerInfos;
                                 }
                             }
                             
                             // 验证页面确实有数据
                             List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                             if (items.size() == 0) {
                                 items = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
                             }
                             
                             if (items.size() > 0) {
                                 System.out.println("✅ 第" + targetPage + "页验证成功，找到 " + items.size() + " 个商品");
                             } else {
                                 System.err.println("❌ 第" + targetPage + "页没有商品，翻页失败");
                                 System.err.println("❌ 无法到达第" + progress.getCurrentPage() + "页，爬取终止");
                                 return manufacturerInfos;
                             }
                             
                             // 如果不是最后一页，执行页面初始化流程
                             if (targetPage < progress.getCurrentPage()) {
                                 System.out.println("🔄 第" + targetPage + "页不是目标页，执行页面初始化流程...");
                                 
                                 // 1. 执行反检测脚本
                                 System.out.println("🔧 执行反检测脚本...");
                                 antiDetectionService.executeAntiDetectionScripts(driver);
                                 
                                 // 2. 模拟人类行为
                                 System.out.println("🤖 模拟人类行为...");
                                 antiDetectionService.simulateHumanBehavior(driver);
                                 
                                 // 3. 随机等待
                                 System.out.println("⏰ 随机等待...");
                                 antiDetectionService.randomWait(2000, 4000);
                                 
                                 // 4. 滚动页面
                                 System.out.println("📜 开始滚动页面...");
                                 scrollPage(driver);
                                 System.out.println("✅ 页面滚动完成");
                             }
                             
                         } else {
                             System.err.println("❌ 下一页按钮不可用，无法继续翻页");
                             System.err.println("❌ 无法到达第" + progress.getCurrentPage() + "页，爬取终止");
                             return manufacturerInfos;
                         }
                     } catch (Exception e) {
                         System.err.println("❌ 点击下一页按钮失败: " + e.getMessage());
                         System.err.println("❌ 无法到达第" + progress.getCurrentPage() + "页，爬取终止");
                         return manufacturerInfos;
                     }
                 }
                 
                 System.out.println("🎯 断点续传：已成功逐页点击到第" + progress.getCurrentPage() + "页，准备开始处理");
                 
                 // 到达目标页后，执行完整的页面初始化流程
                 System.out.println("🔄 开始执行目标页初始化流程...");
                 
                 // 1. 执行反检测脚本
                 System.out.println("🔧 执行反检测脚本...");
                 antiDetectionService.executeAntiDetectionScripts(driver);
                 
                 // 2. 模拟人类行为
                 System.out.println("🤖 模拟人类行为...");
                 antiDetectionService.simulateHumanBehavior(driver);
                 
                 // 3. 随机等待
                 System.out.println("⏰ 随机等待...");
                 antiDetectionService.randomWait(2000, 5000);
                 
                 // 4. 滚动页面到底部
                 System.out.println("📜 开始滚动页面到底部...");
                 scrollPage(driver);
                 System.out.println("✅ 页面滚动完成");
                 
                 System.out.println("🎯 目标页初始化流程完成！");
                 
             } else {
                 System.out.println("🔄 从第1页开始，无需翻页");
             }
            
            System.out.println("🔄 断点续传详情:");
            System.out.println("   - 任务表进度: 第" + startPage + "页，第" + startItemIndex + "项");
            System.out.println("   - 进度表进度: 第" + (existingProgress.isPresent() ? existingProgress.get().getCurrentPage() : "N/A") + "页，第" + (existingProgress.isPresent() ? existingProgress.get().getCurrentItemIndex() : "N/A") + "项");
            System.out.println("   - 最终使用进度: 第" + progress.getCurrentPage() + "页，第" + progress.getCurrentItemIndex() + "项");
            
            for (int page = progress.getCurrentPage(); page <= maxPages; page++) {
                System.out.println("📄 ========== 开始处理第 " + page + " 页 ==========");
                
                                 // 🆕 简化逻辑：断点导航已在外层处理，这里只需要处理正常翻页
                 if (page > 1 && page > progress.getCurrentPage()) {
                     System.out.println("🔄 需要从第" + page + "页开始，先翻页到指定页面...");
                     
                     // 使用点击下一页按钮的方式翻页到目标页面
                     if (!navigateToPageByClicking(driver, wait, page)) {
                         System.err.println("❌ 无法翻页到第" + page + "页，爬取终止");
                         break;
                     }
                     
                     // 🆕 修复：翻页成功后，更新进度到当前页，但保持商品索引（支持断点续传）
                     progress.setCurrentPage(page); // 更新到当前页
                     System.out.println("🔄 翻页成功后，保持断点续传商品索引: " + progress.getCurrentItemIndex());
                     // 🆕 使用统一方法同步更新两个表
                     updateProgressPublic(progress, page, progress.getCurrentItemIndex(), "IN_PROGRESS");
                 }
                
                // 🆕 修复：页面开始处理时，更新状态和页码，但保留商品索引（支持断点续传）
                progress.setStatus("IN_PROGRESS");
                progress.setCurrentPage(page);
                // 注意：不要重置商品索引，保持断点续传的状态
                // progress.setCurrentItemIndex(0); // 删除这行，避免覆盖断点续传的商品索引
                // 🆕 使用统一方法同步更新两个表
                updateProgressPublic(progress, page, progress.getCurrentItemIndex(), "IN_PROGRESS");

                            // 🆕 检查当前页面是否出现验证码
            if (captchaHandler.checkForCaptcha(driver)) {
                System.out.println("📄 页面内检测到验证码，尝试处理...");
                CaptchaHandlerService.CaptchaResult result = captchaHandler.handleCaptcha(driver);
                if (result == CaptchaHandlerService.CaptchaResult.SUCCESS) {
                    System.out.println("📄 页面内验证码处理成功，继续...");
                } else if (result == CaptchaHandlerService.CaptchaResult.FAILED) {
                    System.err.println("📄 页面内验证码处理失败，需要重新加载页面...");
                    // 🆕 验证码处理失败，重新加载当前页面
                    driver.navigate().refresh();
                    antiDetectionService.randomWait(3000, 5000);
                    // 重新执行反检测和人类行为模拟
                    antiDetectionService.executeAntiDetectionScripts(driver);
                    antiDetectionService.simulateHumanBehavior(driver);
                    scrollPage(driver);
                    // 重新检查验证码
                    if (captchaHandler.checkForCaptcha(driver)) {
                        System.err.println("📄 重新加载后仍有验证码，尝试再次处理...");
                        result = captchaHandler.handleCaptcha(driver);
                        if (result == CaptchaHandlerService.CaptchaResult.BLOCKED) {
                            System.err.println("📄 页面内验证码被阻止，跳过当前页面");
                            break; // 跳过当前页面，继续下一页
                        }
                    }
                } else {
                    System.err.println("📄 页面内验证码被阻止，跳过当前页面");
                    break; // 跳过当前页面，继续下一页
                }
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

                // 🆕 修复：确定开始处理的商品索引（支持断点续传）
                // 如果是断点续传的第一页，从上次中断的商品索引开始；否则从0开始
                int startIndex;
                System.out.println("🔍 断点续传判断:");
                System.out.println("   - 当前页码: " + page);
                System.out.println("   - 进度表页码: " + progress.getCurrentPage());
                System.out.println("   - 进度表商品索引: " + progress.getCurrentItemIndex());
                System.out.println("   - 条件1 (page == progress.getCurrentPage()): " + (page == progress.getCurrentPage()));
                System.out.println("   - 条件2 (progress.getCurrentItemIndex() > 0): " + (progress.getCurrentItemIndex() > 0));
                
                if (page == progress.getCurrentPage() && progress.getCurrentItemIndex() > 0) {
                    // 断点续传：从上次中断的商品索引开始
                    startIndex = progress.getCurrentItemIndex();
                    System.out.println("📄 第" + page + "页 - 🔄 断点续传：从第" + (startIndex + 1) + "个商品开始");
                } else {
                    // 新页面：从第一个商品开始
                    startIndex = 0;
                    System.out.println("📄 第" + page + "页 - 🆕 新页面：从第1个商品开始");
                }
                System.out.println("📄 第" + page + "页 - 🔍 开始处理，起始索引: " + startIndex + "，商品总数: " + items.size());
                
                for (int i = startIndex; i < items.size(); i++) {
                    try {
                        // 🆕 修复：更新当前处理的商品索引，使用正确的页码
                        progress.setCurrentItemIndex(i);
                        progress.setCurrentPage(page); // 确保页码正确
                        
                        // 🆕 添加调试信息
                        System.out.println("🔄 准备调用 updateProgressPublic:");
                        System.out.println("   - 进度对象ID: " + progress.getId());
                        System.out.println("   - 页码: " + page);
                        System.out.println("   - 项索引: " + i);
                        System.out.println("   - 进度对象页码: " + progress.getCurrentPage());
                        System.out.println("   - 进度对象项索引: " + progress.getCurrentItemIndex());
                        
                        // 🆕 使用统一方法同步更新两个表
                        updateProgressPublic(progress, page, i, "IN_PROGRESS");
                        
                        System.out.println("📄 第" + page + "页 - 🎯 开始处理第 " + (i + 1) + " 个商品...");
                        
                        // 验证WebDriver会话是否仍然有效
                        try {
                            driver.getTitle(); // 简单的会话验证
                        } catch (Exception sessionEx) {
                            System.err.println("📄 第" + page + "页 - ❌ WebDriver会话已失效: " + sessionEx.getMessage());
                            System.err.println("📄 第" + page + "页 - ❌ 爬取终止，请重启程序");
                            // 🆕 使用统一方法同步更新两个表
                            updateProgressPublic(progress, page, i, "FAILED");
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
                            System.out.println("📄 联系方式页面检测到验证码，尝试处理...");
                            CaptchaHandlerService.CaptchaResult result = captchaHandler.handleCaptcha(driver);
                            if (result == CaptchaHandlerService.CaptchaResult.SUCCESS) {
                                System.out.println("📄 联系方式页面验证码处理成功，继续...");
                            } else if (result == CaptchaHandlerService.CaptchaResult.FAILED) {
                                System.err.println("📄 联系方式页面验证码处理失败，需要重新爬取当前商品...");
                                // 🆕 验证码处理失败，回到主页面重新爬取当前商品
                                driver.navigate().back();
                                antiDetectionService.randomWait(2000, 3000);
                                
                                // 重新进入商品详情页
                                try {
                                    WebElement productLink = driver.findElement(By.xpath("//a[contains(@href, 'offer') and contains(@href, '.html')]"));
                                    if (productLink != null) {
                                        System.out.println("🔄 重新进入商品详情页...");
                                        productLink.click();
                                        antiDetectionService.randomWait(3000, 5000);
                                        
                                        // 重新执行反检测和人类行为模拟
                                        antiDetectionService.executeAntiDetectionScripts(driver);
                                        antiDetectionService.simulateHumanBehavior(driver);
                                        scrollPage(driver);
                                        
                                        // 重新检查验证码
                                        if (captchaHandler.checkForCaptcha(driver)) {
                                            System.err.println("📄 重新进入后仍有验证码，尝试再次处理...");
                                            result = captchaHandler.handleCaptcha(driver);
                                            if (result == CaptchaHandlerService.CaptchaResult.BLOCKED) {
                                                System.err.println("📄 重新进入后验证码被阻止，跳过当前商品");
                                                continue; // 跳过当前商品，继续下一个
                                            }
                                        }
                                        
                                        // 重新提取联系方式信息
                                        extractContactInfo(driver, info);
                                    } else {
                                        System.err.println("❌ 无法找到商品链接，跳过当前商品");
                                        continue;
                                    }
                                } catch (Exception e) {
                                    System.err.println("❌ 重新进入商品详情页失败: " + e.getMessage());
                                    continue;
                                }
                            } else {
                                System.err.println("📄 联系方式页面验证码被阻止，跳过当前商品");
                                continue; // 跳过当前商品，继续下一个
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
                                System.out.println("📄 联系方式页面检测到验证码，尝试处理...");
                                CaptchaHandlerService.CaptchaResult result = captchaHandler.handleCaptcha(driver);
                                if (result == CaptchaHandlerService.CaptchaResult.SUCCESS) {
                                    System.out.println("📄 联系方式页面验证码处理成功");
                                                        } else if (result == CaptchaHandlerService.CaptchaResult.FAILED) {
                            System.err.println("📄 联系方式页面验证码处理失败，需要重新爬取当前商品...");
                            // 🆕 验证码处理失败，回到主页面重新爬取当前商品
                            driver.navigate().back();
                            antiDetectionService.randomWait(2000, 3000);
                            
                            // 重新进入商品详情页
                            try {
                                WebElement productLink = driver.findElement(By.xpath("//a[contains(@href, 'offer') and contains(@href, '.html')]"));
                                if (productLink != null) {
                                    System.out.println("🔄 重新进入商品详情页...");
                                    productLink.click();
                                    antiDetectionService.randomWait(3000, 5000);
                                    
                                    // 重新执行反检测和人类行为模拟
                                    antiDetectionService.executeAntiDetectionScripts(driver);
                                    antiDetectionService.simulateHumanBehavior(driver);
                                    scrollPage(driver);
                                    
                                    // 重新检查验证码
                                    if (captchaHandler.checkForCaptcha(driver)) {
                                        System.err.println("📄 重新进入后仍有验证码，尝试再次处理...");
                                        result = captchaHandler.handleCaptcha(driver);
                                        if (result == CaptchaHandlerService.CaptchaResult.BLOCKED) {
                                            System.err.println("📄 重新进入后验证码被阻止，跳过当前商品");
                                            continue; // 跳过当前商品，继续下一个
                                        }
                                    }
                                    
                                    // 重新提取联系方式信息
                                    extractContactInfo(driver, info);
                                } else {
                                    System.err.println("❌ 无法找到商品链接，跳过当前商品");
                                    continue;
                                }
                            } catch (Exception e) {
                                System.err.println("❌ 重新进入商品详情页失败: " + e.getMessage());
                                continue;
                            }
                        } else {
                            System.err.println("📄 联系方式页面验证码被阻止，跳过此商品");
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
                                    
                                                                         // 🆕 使用统一方法同步更新两个表
                                    updateProgressPublic(progress, page, i, "IN_PROGRESS");
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
                
                // 🆕 记录当前页面的URL，用于断点续传
                String currentPageUrl = driver.getCurrentUrl();
                progress.setCurrentPageUrl(currentPageUrl);
                System.out.println("📄 第" + page + "页 - 🔗 记录当前页面URL: " + currentPageUrl);
                
                // 只有当当前页面所有商品都处理完才翻页
                if (processedItemsOnPage > 0) {
                                                             // 🆕 改进翻页逻辑：连续执行时直接点击下一页按钮
                 if (page < maxPages) {
                     System.out.println("📄 第" + page + "页 - 🔄 准备点击到第 " + (page + 1) + " 页...");
                     
                     // 等待页面加载完成
                     antiDetectionService.randomWait(2000, 3000);
                     
                     // 查找并点击下一页按钮
                     try {
                         WebElement nextPageButton = driver.findElement(By.xpath("//BUTTON[contains(@class,'next-btn next-btn-normal next-btn-large next-pagination-item next')]"));
                         if (nextPageButton != null && nextPageButton.isEnabled()) {
                             System.out.println("📄 第" + page + "页 - 🖱️ 找到下一页按钮，点击...");
                             ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPageButton);
                             
                             // 等待新页面加载
                             System.out.println("📄 第" + (page + 1) + "页 - ⏳ 等待页面加载...");
                             antiDetectionService.randomWait(3000, 5000);
                             
                             // 等待商品列表加载完成
                             try {
                                 wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
                                 System.out.println("📄 第" + (page + 1) + "页 - ✅ 页面商品列表加载完成");
                             } catch (Exception e) {
                                 System.out.println("📄 第" + (page + 1) + "页 - ⚠️ 等待商品列表加载超时，尝试备用选择器...");
                                 try {
                                     wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'offer_item')]")));
                                     System.out.println("📄 第" + (page + 1) + "页 - ✅ 页面商品列表加载完成（备用选择器）");
                                 } catch (Exception e2) {
                                     System.err.println("📄 第" + (page + 1) + "页 - ❌ 页面商品列表加载失败: " + e2.getMessage());
                                     System.out.println("📄 第" + page + "页 - ⚠️ 没有更多页面了");
                                     break;
                                 }
                             }
                         } else {
                             System.err.println("📄 第" + page + "页 - ❌ 下一页按钮不可用，没有更多页面了");
                             break;
                         }
                     } catch (Exception e) {
                         System.err.println("📄 第" + page + "页 - ❌ 点击下一页按钮失败: " + e.getMessage());
                         System.out.println("📄 第" + page + "页 - ⚠️ 没有更多页面了");
                         break;
                     }
                        
                        // 🆕 修复翻页后的进度更新逻辑
                        // 翻页成功后，更新进度到下一页，重置商品索引为0
                        progress.setCurrentPage(page + 1); // 更新到下一页
                        progress.setCurrentItemIndex(0); // 重置为0，从第一个商品开始
                        // 🆕 使用统一方法同步更新两个表
                        updateProgressPublic(progress, page + 1, 0, "IN_PROGRESS");
                        
                        System.out.println("📄 第" + (page + 1) + "页 - 🔄 翻页成功，重置商品索引为0");
                        
                                                 // 🆕 翻页后像刚启动任务一样，重新执行所有必要操作
                         System.out.println("📄 第" + (page + 1) + "页 - 🔄 翻页成功，开始执行新页面初始化流程...");
                         
                         // 1. 等待新页面完全加载
                         System.out.println("📄 第" + (page + 1) + "页 - ⏳ 等待新页面完全加载...");
                         antiDetectionService.randomWait(3000, 5000);
                         
                         // 🆕 检查新页面是否出现验证码
                         if (captchaHandler.checkForCaptcha(driver)) {
                             System.out.println("📄 第" + (page + 1) + "页 - ⚠️ 新页面检测到验证码，尝试处理...");
                             CaptchaHandlerService.CaptchaResult result = captchaHandler.handleCaptcha(driver);
                             if (result == CaptchaHandlerService.CaptchaResult.SUCCESS) {
                                 System.out.println("📄 第" + (page + 1) + "页 - ✅ 新页面验证码处理成功，继续...");
                             } else if (result == CaptchaHandlerService.CaptchaResult.FAILED) {
                                 System.err.println("📄 第" + (page + 1) + "页 - ⚠️ 新页面验证码处理失败，但继续处理此页面...");
                             } else {
                                 System.err.println("📄 第" + (page + 1) + "页 - ❌ 新页面验证码被阻止，跳过此页面");
                                 break;
                             }
                         }
                         
                         // 2. 执行反检测脚本（防止被检测）
                         System.out.println("📄 第" + (page + 1) + "页 - 🔧 执行反检测脚本...");
                         antiDetectionService.executeAntiDetectionScripts(driver);
                         
                         // 3. 模拟人类行为（随机移动鼠标、滚动等）
                         System.out.println("📄 第" + (page + 1) + "页 - 🤖 模拟人类行为...");
                         antiDetectionService.simulateHumanBehavior(driver);
                         
                         // 4. 随机等待，模拟人类行为
                         System.out.println("📄 第" + (page + 1) + "页 - ⏰ 随机等待...");
                         antiDetectionService.randomWait(2000, 5000);
                         
                         // 5. 滚动页面到底部，确保所有商品都加载完成
                         System.out.println("📄 第" + (page + 1) + "页 - 📜 开始滚动新页面到底部...");
                         scrollPage(driver);
                         System.out.println("📄 第" + (page + 1) + "页 - ✅ 新页面滚动完成");
                         
                         // 6. 验证新页面商品列表
                         System.out.println("📄 第" + (page + 1) + "页 - 🔍 验证新页面商品列表...");
                         List<WebElement> newPageItems = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                         if (newPageItems.size() == 0) {
                             newPageItems = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
                         }
                         System.out.println("📄 第" + (page + 1) + "页 - ✅ 新页面验证成功，找到 " + newPageItems.size() + " 个商品");
                         
                         System.out.println("📄 第" + (page + 1) + "页 - 🎯 新页面初始化流程完成，准备开始处理商品...");
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
            // 🆕 使用统一方法同步更新两个表
            if (progress != null) {
                updateProgressPublic(progress, progress.getCurrentPage(), progress.getCurrentItemIndex(), "FAILED");
            }
        } finally {
            // 🆕 使用统一方法同步更新两个表
            if (progress != null) {
                if (progress.getCurrentPage() >= maxPages) {
                    updateProgressPublic(progress, progress.getCurrentPage(), progress.getCurrentItemIndex(), "COMPLETED");
                    System.out.println("🎉 爬取任务完成！");
                } else {
                    updateProgressPublic(progress, progress.getCurrentPage(), progress.getCurrentItemIndex(), "IN_PROGRESS");
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
      * 🆕 通过点击下一页按钮翻页到指定页面
      */
     private boolean navigateToPageByClicking(WebDriver driver, WebDriverWait wait, int targetPage) {
         try {
             System.out.println("🔄 开始通过点击翻页到第" + targetPage + "页...");
             
             // 获取当前页面URL
             String currentUrl = driver.getCurrentUrl();
             System.out.println("🔄 当前页面URL: " + currentUrl);
             
             // 通过点击下一页按钮逐页翻到目标页
             for (int currentPage = 1; currentPage < targetPage; currentPage++) {
                 System.out.println("🔄 当前在第" + currentPage + "页，准备点击到第" + (currentPage + 1) + "页...");
                 
                 // 等待页面加载完成
                 antiDetectionService.randomWait(2000, 3000);
                 
                 // 查找并点击下一页按钮
                 try {
                     WebElement nextPageButton = driver.findElement(By.xpath("//BUTTON[contains(@class,'next-btn next-btn-normal next-btn-large next-pagination-item next')]"));
                     if (nextPageButton != null && nextPageButton.isEnabled()) {
                         System.out.println("🔄 找到下一页按钮，点击...");
                         ((JavascriptExecutor) driver).executeScript("arguments[0].click();", nextPageButton);
                         
                         // 等待新页面加载
                         System.out.println("⏳ 等待第" + (currentPage + 1) + "页加载...");
                         antiDetectionService.randomWait(3000, 5000);
                         
                         // 等待商品列表加载完成
                         try {
                             wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
                             System.out.println("✅ 第" + (currentPage + 1) + "页商品列表加载完成");
                         } catch (Exception e) {
                             System.out.println("⚠️ 等待第" + (currentPage + 1) + "页商品列表加载超时，尝试备用选择器...");
                             try {
                                 wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'offer_item')]")));
                                 System.out.println("✅ 第" + (currentPage + 1) + "页商品列表加载完成（备用选择器）");
                             } catch (Exception e2) {
                                 System.err.println("❌ 第" + (currentPage + 1) + "页商品列表加载失败: " + e2.getMessage());
                                 return false;
                             }
                         }
                         
                         // 验证页面确实有数据
                         List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
                         if (items.size() == 0) {
                             items = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
                         }
                         
                         if (items.size() > 0) {
                             System.out.println("✅ 第" + (currentPage + 1) + "页验证成功，找到 " + items.size() + " 个商品");
                         } else {
                             System.err.println("❌ 第" + (currentPage + 1) + "页没有商品，翻页失败");
                             return false;
                         }
                         
                         // 如果不是最后一页，执行页面初始化流程
                         if (currentPage + 1 < targetPage) {
                             System.out.println("🔄 第" + (currentPage + 1) + "页不是目标页，执行页面初始化流程...");
                             
                             // 1. 执行反检测脚本
                             System.out.println("🔧 执行反检测脚本...");
                             antiDetectionService.executeAntiDetectionScripts(driver);
                             
                             // 2. 模拟人类行为
                             System.out.println("🤖 模拟人类行为...");
                             antiDetectionService.simulateHumanBehavior(driver);
                             
                             // 3. 随机等待
                             System.out.println("⏰ 随机等待...");
                             antiDetectionService.randomWait(2000, 4000);
                             
                             // 4. 滚动页面
                             System.out.println("📜 开始滚动页面...");
                             scrollPage(driver);
                             System.out.println("✅ 页面滚动完成");
                         }
                         
                     } else {
                         System.err.println("❌ 下一页按钮不可用，无法继续翻页");
                         return false;
                     }
                 } catch (Exception e) {
                     System.err.println("❌ 点击下一页按钮失败: " + e.getMessage());
                     return false;
                 }
             }
             
             System.out.println("🎯 成功通过点击翻页到第" + targetPage + "页！");
             return true;
             
         } catch (Exception e) {
             System.err.println("❌ 通过点击翻页到第" + targetPage + "页时出错: " + e.getMessage());
             return false;
         }
     }
     
     /**
      * 🆕 改进翻页逻辑：记录每页URL，直接访问而不是连续翻页
      */
     private boolean navigateToPage(WebDriver driver, WebDriverWait wait, int targetPage) {
        try {
            System.out.println("🔄 开始导航到第" + targetPage + "页...");
            
            // 获取当前页面URL
            String currentUrl = driver.getCurrentUrl();
            System.out.println("🔄 当前页面URL: " + currentUrl);
            
            // 构建目标页面URL
            String targetUrl = buildPageUrl(currentUrl, targetPage);
            System.out.println("🔄 目标页面URL: " + targetUrl);
            
            if (targetUrl.equals(currentUrl)) {
                System.out.println("✅ 当前已在目标页面，无需导航");
                return true;
            }
            
            // 直接访问目标页面URL
            System.out.println("🔄 直接访问目标页面URL...");
            driver.get(targetUrl);
            
            // 等待页面加载
            System.out.println("⏳ 等待页面加载...");
            antiDetectionService.randomWait(3000, 5000);
            
            // 等待商品列表加载完成
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
                System.out.println("✅ 页面商品列表加载完成");
            } catch (Exception e) {
                System.out.println("⚠️ 等待商品列表加载超时，尝试备用选择器...");
                try {
                    wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'offer_item')]")));
                    System.out.println("✅ 页面商品列表加载完成（备用选择器）");
                } catch (Exception e2) {
                    System.err.println("❌ 页面商品列表加载失败: " + e2.getMessage());
                    return false;
                }
            }
            
            // 🆕 翻页成功后，像刚启动任务一样，重新执行所有必要操作
            System.out.println("🔄 页面加载成功，开始执行新页面初始化流程...");
            
            // 1. 执行反检测脚本（防止被检测）
            System.out.println("🔧 执行反检测脚本...");
            antiDetectionService.executeAntiDetectionScripts(driver);
            
            // 2. 模拟人类行为（随机移动鼠标、滚动等）
            System.out.println("🤖 模拟人类行为...");
            antiDetectionService.simulateHumanBehavior(driver);
            
            // 3. 随机等待，模拟人类行为
            System.out.println("⏰ 随机等待...");
            antiDetectionService.randomWait(2000, 5000);
            
            // 4. 滚动页面到底部，确保所有商品都加载完成
            System.out.println("📜 开始滚动新页面到底部...");
            scrollPage(driver);
            System.out.println("✅ 页面滚动完成");
            
            // 5. 验证新页面商品列表
            System.out.println("🔍 验证新页面商品列表...");
            List<WebElement> newPageItems = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
            if (newPageItems.size() == 0) {
                newPageItems = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
            }
            System.out.println("✅ 新页面验证成功，找到 " + newPageItems.size() + " 个商品");
            
            System.out.println("🎯 新页面初始化流程完成！");
            System.out.println("🎯 成功导航到第" + targetPage + "页！");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 导航到第" + targetPage + "页时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 🆕 构建指定页面的URL
     */
    private String buildPageUrl(String baseUrl, int pageNumber) {
        try {
            if (pageNumber <= 1) {
                // 第1页，移除page参数或设置为1
                if (baseUrl.contains("page=")) {
                    return baseUrl.replaceAll("page=\\d+", "page=1");
                } else {
                    // 如果没有page参数，添加page=1
                    String separator = baseUrl.contains("?") ? "&" : "?";
                    return baseUrl + separator + "page=1";
                }
            } else {
                // 第2页及以后，设置或更新page参数
                if (baseUrl.contains("page=")) {
                    return baseUrl.replaceAll("page=\\d+", "page=" + pageNumber);
                } else {
                    // 如果没有page参数，添加page参数
                    String separator = baseUrl.contains("?") ? "&" : "?";
                    return baseUrl + separator + "page=" + pageNumber;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 构建页面URL失败: " + e.getMessage());
            return baseUrl; // 返回原URL作为备选
        }
    }
    
    /**
     * 获取当前页面的页码
     */
    private int getCurrentPageNumber(WebDriver driver) {
        try {
            System.out.println("🔍 开始检测当前页码...");
            
            // 方法1：从分页按钮获取当前页码
            try {
                List<WebElement> pageElements = driver.findElements(By.xpath("//button[contains(@class, 'next-pagination-item') and not(contains(@class, 'next'))]"));
                System.out.println("🔍 找到 " + pageElements.size() + " 个分页按钮");
                
                for (WebElement element : pageElements) {
                    try {
                        String text = element.getText().trim();
                        System.out.println("🔍 分页按钮文本: '" + text + "'");
                        if (text.matches("\\d+")) {
                            int pageNum = Integer.parseInt(text);
                            System.out.println("🔍 从分页按钮检测到当前页码: " + pageNum);
                            return pageNum;
                        }
                    } catch (Exception e) {
                        System.out.println("🔍 解析分页按钮文本失败: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("🔍 方法1失败: " + e.getMessage());
            }
            
            // 方法2：从URL参数获取页码
            try {
                String currentUrl = driver.getCurrentUrl();
                System.out.println("🔍 当前URL: " + currentUrl);
                
                // 查找URL中的页码参数
                if (currentUrl.contains("page=")) {
                    Pattern pattern = Pattern.compile("page=(\\d+)");
                    Matcher matcher = pattern.matcher(currentUrl);
                    if (matcher.find()) {
                        int pageNum = Integer.parseInt(matcher.group(1));
                        System.out.println("🔍 从URL参数检测到当前页码: " + pageNum);
                        return pageNum;
                    }
                }
            } catch (Exception e) {
                System.out.println("🔍 方法2失败: " + e.getMessage());
            }
            
            // 方法3：从页面标题或其他元素推断
            try {
                String pageTitle = driver.getTitle();
                System.out.println("🔍 页面标题: " + pageTitle);
                
                // 如果标题包含页码信息
                if (pageTitle.contains("第") && pageTitle.contains("页")) {
                    Pattern pattern = Pattern.compile("第(\\d+)页");
                    Matcher matcher = pattern.matcher(pageTitle);
                    if (matcher.find()) {
                        int pageNum = Integer.parseInt(matcher.group(1));
                        System.out.println("🔍 从页面标题检测到当前页码: " + pageNum);
                        return pageNum;
                    }
                }
            } catch (Exception e) {
                System.out.println("🔍 方法3失败: " + e.getMessage());
            }
            
            // 方法4：从页面内容推断（查找"第X页"文本）
            try {
                List<WebElement> pageTextElements = driver.findElements(By.xpath("//*[contains(text(), '第') and contains(text(), '页')]"));
                for (WebElement element : pageTextElements) {
                    try {
                        String text = element.getText().trim();
                        System.out.println("🔍 找到页面文本: '" + text + "'");
                        Pattern pattern = Pattern.compile("第(\\d+)页");
                        Matcher matcher = pattern.matcher(text);
                        if (matcher.find()) {
                            int pageNum = Integer.parseInt(matcher.group(1));
                            System.out.println("🔍 从页面文本检测到当前页码: " + pageNum);
                            return pageNum;
                        }
                    } catch (Exception e) {
                        // 忽略单个元素的错误
                    }
                }
            } catch (Exception e) {
                System.out.println("🔍 方法4失败: " + e.getMessage());
            }
            
            System.out.println("⚠️ 所有方法都无法检测到页码，使用默认值1");
            return 1;
            
        } catch (Exception e) {
            System.err.println("❌ 获取当前页码失败: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * 验证翻页后页面是否真的发生了变化
     */
    private boolean verifyPageChanged(WebDriver driver, WebDriverWait wait) {
        try {
            // 等待页面加载完成
            antiDetectionService.randomWait(1000, 2000);
            
            // 检查商品列表是否存在
            List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
            if (items.size() == 0) {
                // 尝试备用选择器
                items = driver.findElements(By.xpath("//div[contains(@class, 'offer_item')]"));
            }
            
            if (items.size() > 0) {
                System.out.println("✅ 翻页验证成功，新页面找到 " + items.size() + " 个商品");
                return true;
            } else {
                System.err.println("❌ 翻页验证失败，新页面没有找到商品");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ 翻页验证过程出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 🆕 统一更新两个表的进度信息，确保同步
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateBothTablesProgress(CrawlProgress progress, int currentPage, int currentItemIndex, String status) {
        try {
            System.out.println("🔄 开始同步更新两个表的进度信息...");
            System.out.println("   - 目标页码: " + currentPage);
            System.out.println("   - 目标项索引: " + currentItemIndex);
            System.out.println("   - 目标状态: " + status);
            System.out.println("   - 进度记录ID: " + progress.getId());
            System.out.println("   - 任务ID: " + progress.getTaskId());
            
            // 🆕 添加服务实例检查
            if (crawlProgressService == null) {
                System.err.println("❌ crawlProgressService 为 null！");
                return;
            }
            if (crawlTaskService == null) {
                System.err.println("❌ crawlTaskService 为 null！");
                return;
            }
            System.out.println("✅ 服务实例检查通过");
            
            // 1. 更新进度表
            System.out.println("📊 更新进度表...");
            CrawlProgress updatedProgress = crawlProgressService.updateProgress(progress.getId(), currentPage, currentItemIndex, status);
            if (updatedProgress != null) {
                System.out.println("✅ 进度表更新成功");
                System.out.println("   - 更新后页码: " + updatedProgress.getCurrentPage());
                System.out.println("   - 更新后项索引: " + updatedProgress.getCurrentItemIndex());
            } else {
                System.err.println("❌ 进度表更新失败，返回null");
            }
            
            // 2. 同步更新任务表（如果有任务ID）
            if (progress.getTaskId() != null) {
                System.out.println("📊 同步更新任务表...");
                System.out.println("   - 调用 updateTaskProgress(" + progress.getTaskId() + ", " + currentPage + ", " + currentItemIndex + ")");
                
                // 🆕 直接调用任务表更新，不进行重试（让事务管理处理）
                try {
                    crawlTaskService.updateTaskProgress(progress.getTaskId(), currentPage, currentItemIndex);
                    System.out.println("✅ 任务表更新调用成功");
                    
                } catch (Exception e) {
                    System.err.println("❌ 任务表更新失败: " + e.getMessage());
                    e.printStackTrace();
                    // 🆕 如果任务表更新失败，抛出异常让事务回滚
                    throw new RuntimeException("任务表更新失败: " + e.getMessage(), e);
                }
            } else {
                System.out.println("⚠️ 进度记录没有关联任务ID，跳过任务表更新");
            }
            
            System.out.println("🎯 两个表进度同步更新完成！");
            
        } catch (Exception e) {
            System.err.println("❌ 同步更新两个表进度失败: " + e.getMessage());
            e.printStackTrace();
            throw e; // 重新抛出异常，确保事务回滚
        }
    }
    
    /**
     * 🆕 公共方法：调用统一进度更新，确保@Transactional生效
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProgressPublic(CrawlProgress progress, int currentPage, int currentItemIndex, String status) {
        updateBothTablesProgress(progress, currentPage, currentItemIndex, status);
    }
}