package com.example.demo;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class PageStructureTest {

    public static void main(String[] args) {
        System.out.println("=== 页面结构测试 ===");
        
        // 设置WebDriver
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-web-security");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        
        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        
        try {
            String testUrl = "https://www.1688.com/zw/page.html?spm=a312h.2018_new_sem.dh_001.2.2f6f5576Ce3nO9&hpageId=old-sem-pc-list&cosite=baidujj_pz&keywords=%E5%90%88%E8%82%A5%E9%94%82%E7%94%B5%E6%B1%A0%E7%BB%84&trackid=885662561117990122602&location=re&ptid=01770000000464ce963d082f6fbe7ca7&exp=pcSemFumian%3AC%3BpcDacuIconExp%3AA%3BpcCpxGuessExp%3AB%3BpcCpxCpsExp%3AB%3Bqztf%3AE%3Bwysiwyg%3AB%3BhotBangdanExp%3AB%3BpcSemWwClick%3AA%3BpcSemDownloadPlugin%3AA%3Basst%3AF&sortType=&descendOrder=&province=&city=&priceStart=&priceEnd=&dis=&provinceValue=%E6%89%80%E5%9C%A8%E5%9C%B0%E5%8C%BA&p_rs=true";
            
            System.out.println("正在访问页面: " + testUrl);
            driver.get(testUrl);
            
            // 等待页面加载
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]")));
            
            System.out.println("页面加载完成，开始分析结构...");
            
            // 获取商品列表
            List<WebElement> items = driver.findElements(By.xpath("//div[contains(@class, 'new_ui_offer') and contains(@class, 'offer_item')]"));
            System.out.println("找到 " + items.size() + " 个商品");
            
            if (!items.isEmpty()) {
                WebElement firstItem = items.get(0);
                System.out.println("\n=== 第一个商品的结构分析 ===");
                
                // 查找所有链接
                List<WebElement> links = firstItem.findElements(By.xpath(".//a"));
                System.out.println("找到 " + links.size() + " 个链接");
                
                for (int i = 0; i < Math.min(5, links.size()); i++) {
                    WebElement link = links.get(i);
                    String href = link.getAttribute("href");
                    String text = link.getText();
                    String className = link.getAttribute("class");
                    
                    System.out.println("链接 " + (i+1) + ":");
                    System.out.println("  href: " + href);
                    System.out.println("  text: " + text);
                    System.out.println("  class: " + className);
                    System.out.println();
                }
                
                // 查找所有div元素
                List<WebElement> divs = firstItem.findElements(By.xpath(".//div"));
                System.out.println("找到 " + divs.size() + " 个div元素");
                
                // 查找包含特定文本的元素
                List<WebElement> companyElements = firstItem.findElements(By.xpath(".//*[contains(text(), '公司') or contains(text(), '企业')]"));
                System.out.println("找到 " + companyElements.size() + " 个包含'公司'或'企业'的元素");
                
                for (WebElement element : companyElements) {
                    System.out.println("公司相关元素: " + element.getText());
                }
            }
            
        } catch (Exception e) {
            System.err.println("测试过程中出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
} 