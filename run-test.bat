@echo off
echo 启动1688爬虫测试...
echo.

REM 运行测试
echo 运行爬虫测试...
call mvnw test -Dtest=CrawlerDirectTest#testCrawler

echo.
echo 测试完成！
pause 