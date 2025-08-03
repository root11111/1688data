@echo off
echo 设置Java环境...
set JAVA_HOME=D:\soft\jdk17

echo 编译项目...
call mvnw compile

echo 运行爬虫测试...
call mvnw exec:java -Dexec.mainClass="com.example.demo.SimpleCrawlerTest" -Dexec.args=""

pause 