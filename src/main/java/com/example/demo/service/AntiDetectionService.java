package com.example.demo.service;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AntiDetectionService {

    /**
     * 配置增强的反检测Chrome选项
     */
    public ChromeOptions getEnhancedChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        
        // 基础反检测设置
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-web-security");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        
        // 增强的反检测设置
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-plugins");
        options.addArguments("--disable-images");
        options.addArguments("--disable-javascript");
        options.addArguments("--disable-web-security");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-features=TranslateUI");
        options.addArguments("--disable-ipc-flooding-protection");
        options.addArguments("--disable-background-networking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-sync");
        options.addArguments("--disable-translate");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--mute-audio");
        options.addArguments("--no-first-run");
        options.addArguments("--safebrowsing-disable-auto-update");
        options.addArguments("--disable-client-side-phishing-detection");
        options.addArguments("--disable-component-update");
        options.addArguments("--disable-domain-reliability");
        options.addArguments("--disable-features=AudioServiceOutOfProcess");
        options.addArguments("--disable-hang-monitor");
        options.addArguments("--disable-prompt-on-repost");
        options.addArguments("--disable-sync-preferences");
        options.addArguments("--disable-web-resources");
        options.addArguments("--disable-features=VizDisplayCompositor");
        options.addArguments("--disable-features=TranslateUI");
        options.addArguments("--disable-features=BlinkGenPropertyTrees");
        options.addArguments("--disable-features=ImprovedCookieControls");
        options.addArguments("--disable-features=MediaRouter");
        options.addArguments("--disable-features=OptimizationHints");
        options.addArguments("--disable-features=CalculateNativeWinOcclusion");
        options.addArguments("--disable-features=GlobalMediaControls");
        
        // 新增的反检测设置
        options.addArguments("--disable-features=AutofillServerCommunication");
        options.addArguments("--disable-features=OptimizationHints");
        options.addArguments("--disable-features=Translate");
        options.addArguments("--disable-features=WebRtcHideLocalIpsWithMdns");
        options.addArguments("--disable-features=WebRtcUseEchoCanceller3");
        options.addArguments("--disable-features=WebRtcUseMinMaxVEADimensions");
        options.addArguments("--disable-features=WebRtcUseWifiForAec");
        options.addArguments("--disable-features=WebRtcUseWifiForAgc");
        options.addArguments("--disable-features=WebRtcUseWifiForNs");
        options.addArguments("--disable-features=WebRtcUseWifiForVad");
        options.addArguments("--disable-features=WebRtcUseWifiForVad");
        options.addArguments("--disable-features=WebRtcUseWifiForVad");
        options.addArguments("--disable-features=WebRtcUseWifiForVad");
        options.addArguments("--disable-features=WebRtcUseWifiForVad");
        options.addArguments("--disable-features=WebRtcUseWifiForVad");
        
        // 设置实验性功能
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);
        
        // 设置用户代理
        String userAgent = getRandomUserAgent();
        options.addArguments("--user-agent=" + userAgent);
        
        return options;
    }

    /**
     * 获取随机用户代理
     */
    private String getRandomUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/118.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        };
        
        Random random = new Random();
        return userAgents[random.nextInt(userAgents.length)];
    }

    /**
     * 执行反检测脚本 - 增强版本
     */
    public void executeAntiDetectionScripts(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // 基础反检测
            js.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");
            js.executeScript("Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]})");
            js.executeScript("Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en']})");
            js.executeScript("window.chrome = {runtime: {}}");
            js.executeScript("Object.defineProperty(navigator, 'permissions', {get: () => ({query: () => Promise.resolve({state: 'granted'})})})");
            
            // 增强的反检测脚本
            String antiDetectionScript = 
                "// 移除webdriver属性\n" +
                "delete Object.getPrototypeOf(navigator).webdriver;\n" +
                "\n" +
                "// 修改navigator属性\n" +
                "Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => 8});\n" +
                "Object.defineProperty(navigator, 'deviceMemory', {get: () => 8});\n" +
                "Object.defineProperty(navigator, 'platform', {get: () => 'Win32'});\n" +
                "Object.defineProperty(navigator, 'vendor', {get: () => 'Google Inc.'});\n" +
                "Object.defineProperty(navigator, 'userAgent', {get: () => navigator.userAgent});\n" +
                "\n" +
                "// 修改screen属性\n" +
                "Object.defineProperty(screen, 'width', {get: () => 1920});\n" +
                "Object.defineProperty(screen, 'height', {get: () => 1080});\n" +
                "Object.defineProperty(screen, 'availWidth', {get: () => 1920});\n" +
                "Object.defineProperty(screen, 'availHeight', {get: () => 1040});\n" +
                "Object.defineProperty(screen, 'colorDepth', {get: () => 24});\n" +
                "Object.defineProperty(screen, 'pixelDepth', {get: () => 24});\n" +
                "\n" +
                "// 修改chrome属性\n" +
                "window.chrome = {\n" +
                "    runtime: {},\n" +
                "    loadTimes: function() { return {}; },\n" +
                "    csi: function() { return {}; },\n" +
                "    app: {}\n" +
                "};\n" +
                "\n" +
                "// 修改webgl属性\n" +
                "const getParameter = WebGLRenderingContext.prototype.getParameter;\n" +
                "WebGLRenderingContext.prototype.getParameter = function(parameter) {\n" +
                "    if (parameter === 37445) {\n" +
                "        return 'Intel Inc.';\n" +
                "    }\n" +
                "    if (parameter === 37446) {\n" +
                "        return 'Intel(R) HD Graphics 620';\n" +
                "    }\n" +
                "    return getParameter.call(this, parameter);\n" +
                "};\n" +
                "\n" +
                "// 修改canvas指纹\n" +
                "const originalGetContext = HTMLCanvasElement.prototype.getContext;\n" +
                "HTMLCanvasElement.prototype.getContext = function(type, attributes) {\n" +
                "    const context = originalGetContext.call(this, type, attributes);\n" +
                "    if (type === '2d') {\n" +
                "        const originalFillText = context.fillText;\n" +
                "        context.fillText = function(text, x, y, maxWidth) {\n" +
                "            return originalFillText.call(this, text, x, y, maxWidth);\n" +
                "        };\n" +
                "    }\n" +
                "    return context;\n" +
                "};\n" +
                "\n" +
                "// 修改音频指纹\n" +
                "const originalGetChannelData = AudioBuffer.prototype.getChannelData;\n" +
                "AudioBuffer.prototype.getChannelData = function(channel) {\n" +
                "    const data = originalGetChannelData.call(this, channel);\n" +
                "    return data;\n" +
                "};\n" +
                "\n" +
                "// 修改字体检测\n" +
                "Object.defineProperty(document, 'fonts', {\n" +
                "    get: function() {\n" +
                "        return {\n" +
                "            ready: Promise.resolve(),\n" +
                "            check: function() { return [true, true, true, true, true]; },\n" +
                "            load: function() { return Promise.resolve(); }\n" +
                "        };\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "// 修改媒体设备\n" +
                "Object.defineProperty(navigator, 'mediaDevices', {\n" +
                "    get: function() {\n" +
                "        return {\n" +
                "            getUserMedia: function() { return Promise.resolve(); },\n" +
                "            enumerateDevices: function() { return Promise.resolve([]); }\n" +
                "        };\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "// 修改电池API\n" +
                "Object.defineProperty(navigator, 'getBattery', {\n" +
                "    get: function() {\n" +
                "        return function() {\n" +
                "            return Promise.resolve({\n" +
                "                charging: true,\n" +
                "                chargingTime: Infinity,\n" +
                "                dischargingTime: Infinity,\n" +
                "                level: 1\n" +
                "            });\n" +
                "        };\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "// 修改连接API\n" +
                "Object.defineProperty(navigator, 'connection', {\n" +
                "    get: function() {\n" +
                "        return {\n" +
                "            effectiveType: '4g',\n" +
                "            rtt: 50,\n" +
                "            downlink: 10,\n" +
                "            saveData: false\n" +
                "        };\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "// 修改性能API\n" +
                "Object.defineProperty(window, 'performance', {\n" +
                "    get: function() {\n" +
                "        const original = window.performance;\n" +
                "        return {\n" +
                "            ...original,\n" +
                "            getEntriesByType: function(type) {\n" +
                "                if (type === 'navigation') {\n" +
                "                    return [{\n" +
                "                        type: 'navigate',\n" +
                "                        redirectCount: 0,\n" +
                "                        transferSize: 0,\n" +
                "                        encodedBodySize: 0,\n" +
                "                        decodedBodySize: 0\n" +
                "                    }];\n" +
                "                }\n" +
                "                return original.getEntriesByType.call(this, type);\n" +
                "            }\n" +
                "        };\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "console.log('反检测脚本执行完成');";
            
            js.executeScript(antiDetectionScript);
            
            System.out.println("✅ 增强反检测脚本执行完成");
        } catch (Exception e) {
            System.err.println("❌ 反检测脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 模拟人类行为 - 增强版本
     */
    public void simulateHumanBehavior(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Random random = new Random();
            
            // 随机鼠标移动
            for (int i = 0; i < 3; i++) {
                int x = random.nextInt(800) + 100;
                int y = random.nextInt(600) + 100;
                
                // 模拟鼠标移动
                String mouseScript = 
                    "var event = new MouseEvent('mousemove', {\n" +
                    "    clientX: " + x + ",\n" +
                    "    clientY: " + y + ",\n" +
                    "    bubbles: true,\n" +
                    "    cancelable: true\n" +
                    "});\n" +
                    "document.dispatchEvent(event);";
                
                js.executeScript(mouseScript);
                
                randomWait(100, 300);
            }
            
            // 随机滚动
            int scrollY = random.nextInt(500);
            js.executeScript("window.scrollTo({top: " + scrollY + ", behavior: 'smooth'})");
            
            // 模拟键盘事件
            String keyboardScript = 
                "var keyEvent = new KeyboardEvent('keydown', {\n" +
                "    key: 'Tab',\n" +
                "    code: 'Tab',\n" +
                "    keyCode: 9,\n" +
                "    which: 9,\n" +
                "    bubbles: true,\n" +
                "    cancelable: true\n" +
                "});\n" +
                "document.dispatchEvent(keyEvent);";
            
            js.executeScript(keyboardScript);
            
            // 模拟触摸事件（移动设备检测）
            String touchScript = 
                "var touchEvent = new TouchEvent('touchstart', {\n" +
                "    touches: [{\n" +
                "        clientX: 100,\n" +
                "        clientY: 100,\n" +
                "        identifier: 0,\n" +
                "        target: document.body\n" +
                "    }],\n" +
                "    bubbles: true,\n" +
                "    cancelable: true\n" +
                "});\n" +
                "document.dispatchEvent(touchEvent);";
            
            js.executeScript(touchScript);
            
            System.out.println("✅ 增强人类行为模拟完成");
        } catch (Exception e) {
            System.err.println("❌ 人类行为模拟失败: " + e.getMessage());
        }
    }

    /**
     * 随机等待
     */
    public void randomWait(int minMs, int maxMs) {
        try {
            Random random = new Random();
            int waitTime = random.nextInt(maxMs - minMs + 1) + minMs;
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
} 