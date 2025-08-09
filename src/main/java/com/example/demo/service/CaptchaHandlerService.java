package com.example.demo.service;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Random;

@Service
public class CaptchaHandlerService {

    /**
     * 检查是否存在验证码
     */
    public boolean checkForCaptcha(WebDriver driver) {
        try {
            // 扩展的验证码检测选择器
            List<String> captchaSelectors = List.of(
                // 1688特定的验证码选择器
                "//div[contains(@class, 'nc_wrapper')]",
                "//div[contains(@class, 'nc_scale')]",
                "//div[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//span[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//span[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_wrapper')]//div[contains(@class, 'nc_scale')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_slider')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]",

                // 通用验证码选择器
                "//div[contains(@class, 'captcha')]",
                "//div[contains(@class, 'slider')]",
                "//div[contains(@class, 'verify')]",
                "//div[contains(@class, 'security')]",
                "//iframe[contains(@src, 'captcha')]",
                "//div[contains(text(), '验证')]",
                "//div[contains(text(), '滑动')]",
                "//div[contains(text(), '安全验证')]",
                "//div[contains(text(), '请完成验证')]",
                "//div[contains(text(), '拖动滑块')]",
                "//div[contains(text(), '滑动验证')]",

                // 更多1688特定的选择器
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_slider')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_track')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_bar')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//span",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//i",

                // 图片验证码选择器
                "//div[contains(@class, 'captcha-img')]",
                "//div[contains(@class, 'captcha-image')]",
                "//img[contains(@src, 'captcha')]",
                "//div[contains(@class, 'captcha')]//img",

                // 安全验证选择器
                "//div[contains(@class, 'security-check')]",
                "//div[contains(@class, 'security-verify')]",
                "//div[contains(@class, 'safety-check')]",
                "//div[contains(@class, 'risk-check')]"
            );

            for (String selector : captchaSelectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.xpath(selector));
                    if (!elements.isEmpty()) {
                        System.out.println("🔍 检测到验证码元素: " + selector);
                        return true;
                    }
                } catch (Exception e) {
                    // 忽略异常，继续检查下一个
                }
            }

            // 检查页面标题是否包含验证相关文字
            String pageTitle = driver.getTitle();
            if (pageTitle.contains("验证") || pageTitle.contains("安全") || pageTitle.contains("captcha") ||
                pageTitle.contains("验证码") || pageTitle.contains("滑动验证")) {
                System.out.println("🔍 页面标题包含验证相关文字: " + pageTitle);
                return true;
            }

            // 检查页面源码中是否包含验证码相关文字
            String pageSource = driver.getPageSource();
            if (pageSource.contains("nc_scale") || pageSource.contains("滑动验证") ||
                pageSource.contains("请完成验证") || pageSource.contains("拖动滑块")) {
                System.out.println("🔍 页面源码包含验证码相关文字");
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("检查验证码时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 尝试自动处理滑动验证码 - 改进版本
     */
    public boolean tryAutoHandleSliderCaptcha(WebDriver driver) {
        try {
            System.out.println("🤖 尝试自动处理滑动验证码...");

            // 等待验证码元素加载 - 增加等待时间
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            // 先等待一下，让验证码完全加载
            randomWait(2000, 4000);

            // 尝试多种滑动验证码选择器
            WebElement slider = null;
            WebElement track = null;

            // 选择器列表，按优先级排序
            String[] sliderSelectors = {
                "//div[contains(@class, 'nc_scale')]//span[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_slider')]",
                "//div[contains(@class, 'nc_scale')]//span[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//span",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//i",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//a",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//button",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//img",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//i[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//span[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//span[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_slider')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_track')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_bar')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//span",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//i",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//div",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//a",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//button",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//img",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//i[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//span[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//span[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_iconfont')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_text')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_slider')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_track')]",
                "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_btn')]//div[contains(@class, 'nc_scale_bar')]"
            };

            String[] trackSelectors = {
                    "//div[contains(@class, 'nc_scale')]",
                    "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_track')]",
                    "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_bar')]",
                    "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_text')]",
                    "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_btn')]",
                    "//div[contains(@class, 'nc_scale')]//div[contains(@class, 'nc_scale_slider')]"
            };

            // 查找滑块 - 增加重试机制
            for (int retry = 0; retry < 3; retry++) {
                for (String selector : sliderSelectors) {
                    try {
                        slider = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(selector)));
                        if (slider != null) {
                            System.out.println("✅ 找到滑块元素: " + selector);
                            break;
                        }
                    } catch (Exception e) {
                        // 继续尝试下一个选择器
                    }
                }

                if (slider != null) {
                    break;
                }

                if (retry < 2) {
                    System.out.println("🔄 第 " + (retry + 1) + " 次重试查找滑块...");
                    randomWait(1000, 2000);
                }
            }

            // 查找轨道
            for (String selector : trackSelectors) {
                try {
                    track = driver.findElement(By.xpath(selector));
                    if (track != null) {
                        System.out.println("✅ 找到轨道元素: " + selector);
                        break;
                    }
                } catch (Exception e) {
                    // 继续尝试下一个选择器
                }
            }

            if (slider == null) {
                System.out.println("❌ 未找到滑块元素");
                System.out.println("🔍 尝试查找任何可点击的验证码元素...");

                // 尝试查找任何可点击的验证码相关元素
                try {
                    List<WebElement> clickableElements = driver.findElements(By.xpath("//div[contains(@class, 'nc_')]//*[self::div or self::span or self::button or self::a]"));
                    if (!clickableElements.isEmpty()) {
                        slider = clickableElements.get(0);
                        System.out.println("✅ 找到可点击的验证码元素");
                    }
                } catch (Exception e) {
                    System.out.println("❌ 未找到任何可点击的验证码元素");
                }

                if (slider == null) {
                    return false;
                }
            }

            if (track == null) {
                System.out.println("❌ 未找到轨道元素，使用默认宽度");
                // 使用默认轨道宽度
                track = slider;
            }

            // 获取轨道宽度
            int trackWidth = track.getSize().getWidth();
            System.out.println("📏 滑动轨道宽度: " + trackWidth);

            // 执行滑动 - 模拟人类滑动行为
            Actions actions = new Actions(driver);

            // 移动到滑块
            actions.moveToElement(slider);
            randomWait(500, 1000);

            // 点击并按住
            actions.clickAndHold(slider);
            randomWait(200, 500);

            // 分段滑动，模拟人类行为
            int steps = 15; // 增加步数，使滑动更平滑
            int stepSize = (trackWidth - 20) / steps; // 留一些余量

            System.out.println("🔄 开始滑动，步数: " + steps + ", 步长: " + stepSize);

            for (int i = 0; i < steps; i++) {
                actions.moveByOffset(stepSize, 0);
                randomWait(30, 100); // 减少每步等待时间，使滑动更流畅
            }

            // 释放
            actions.release().perform();

            System.out.println("✅ 自动滑动完成");
            randomWait(2000, 4000);

            // 检查是否成功
            boolean success = !checkForCaptcha(driver);
            if (success) {
                System.out.println("✅ 验证码处理成功！");
            } else {
                System.out.println("❌ 验证码处理失败，可能需要重试");
            }

            return success;

        } catch (Exception e) {
            System.out.println("❌ 自动处理滑动验证码失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 处理验证码 - 主方法（改进版本）
     */
    public boolean handleCaptcha(WebDriver driver) {
        if (!checkForCaptcha(driver)) {
            return true; // 没有验证码
        }

        System.out.println("⚠️  检测到验证码！");

        // 尝试多次自动处理
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.println("🔄 第 " + attempt + " 次尝试自动处理验证码...");

            if (tryAutoHandleSliderCaptcha(driver)) {
                System.out.println("✅ 自动处理验证码成功！");
                return true;
            }

            if (attempt < maxRetries) {
                System.out.println("⏳ 等待后重试...");
                randomWait(2000, 4000);
            }
        }

        // 如果自动处理失败，提示用户手动处理
        System.out.println("❌ 自动处理失败，请手动完成验证码验证");
        System.out.println("💡 提示：");
        System.out.println("   1. 在浏览器中完成滑动验证");
        System.out.println("   2. 如果出现图片验证码，请识别并点击");
        System.out.println("   3. 完成后按回车键继续...");

        return false;
    }

    /**
     * 等待用户手动处理验证码
     */
    public void waitForManualCaptcha() {
        try {
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.println("⏳ 请在浏览器中完成验证码验证，完成后按回车键继续...");
            scanner.nextLine();
            System.out.println("✅ 继续执行爬取...");
        } catch (Exception e) {
            System.err.println("❌ 等待用户输入时出错: " + e.getMessage());
        }
    }

    /**
     * 随机等待
     */
    private void randomWait(int minMs, int maxMs) {
        try {
            Random random = new Random();
            int waitTime = random.nextInt(maxMs - minMs + 1) + minMs;
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}