@echo off
echo ==========================================
echo 1688爬虫项目测试运行器
echo ==========================================

REM 设置Java环境
set JAVA_HOME=D:\soft\jdk17
set PATH=%JAVA_HOME%\bin;%PATH%

REM 设置Maven路径
set MAVEN_HOME=D:\soft\idea\IntelliJ IDEA 2023.1.2\plugins\maven\lib\maven3
set PATH=%MAVEN_HOME%\bin;%PATH%

echo 正在运行所有测试...
echo.

REM 运行所有测试
mvn test

echo.
echo ==========================================
echo 测试完成！
echo ==========================================
pause 