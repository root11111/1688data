@echo off
echo 启动1688爬虫测试...
echo.

REM 使用Maven运行测试
echo 运行爬虫测试...
call mvnw exec:java -Dexec.mainClass="com.example.demo.TestRunner" -Dexec.classpathScope="test"

echo.
echo 测试完成！
pause 