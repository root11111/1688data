@echo off
echo ========================================
echo 1688爬虫测试
echo ========================================
echo.

echo 正在编译项目...
call mvn clean compile -q

if %errorlevel% neq 0 (
    echo ❌ 编译失败，请检查项目配置
    pause
    exit /b 1
)

echo ✅ 编译成功
echo.

echo 正在运行爬虫测试...
echo 注意：如果出现验证码，请手动完成验证后按回车继续
echo.

call mvn exec:java -Dexec.mainClass="com.example.demo.SimpleCrawlerTest" -q

if %errorlevel% neq 0 (
    echo ❌ 爬虫测试失败
    echo.
    echo 💡 故障排除建议：
    echo   1. 检查Chrome浏览器是否已安装
    echo   2. 检查网络连接
    echo   3. 确认目标网站可访问
    echo   4. 查看详细错误日志
) else (
    echo ✅ 爬虫测试完成
)

echo.
pause 