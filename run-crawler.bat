@echo off
echo 启动1688爬虫...
echo.

REM 编译并运行爬虫
echo 编译项目...
call mvnw clean compile

echo 运行爬虫...
call mvnw spring-boot:run -Dspring-boot.run.arguments="--crawl"

echo.
echo 爬虫完成！
pause 