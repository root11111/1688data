# 1688爬虫项目测试运行器
Write-Host "==========================================" -ForegroundColor Green
Write-Host "1688爬虫项目测试运行器" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

# 设置Java环境
$env:JAVA_HOME = "D:\soft\jdk17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 设置Maven路径
$MAVEN_HOME = "D:\soft\idea\IntelliJ IDEA 2023.1.2\plugins\maven\lib\maven3"
$env:PATH = "$MAVEN_HOME\bin;$env:PATH"

Write-Host "正在运行所有测试..." -ForegroundColor Yellow
Write-Host ""

# 运行所有测试
& "$MAVEN_HOME\bin\mvn.cmd" test

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host "测试完成！" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

Read-Host "按任意键继续..." 