Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1688爬虫测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "正在编译项目..." -ForegroundColor Yellow
mvn clean compile -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 编译失败，请检查项目配置" -ForegroundColor Red
    Read-Host "按回车键退出"
    exit 1
}

Write-Host "✅ 编译成功" -ForegroundColor Green
Write-Host ""

Write-Host "正在运行爬虫测试..." -ForegroundColor Yellow
Write-Host "注意：如果出现验证码，请手动完成验证后按回车继续" -ForegroundColor Yellow
Write-Host ""

mvn exec:java -Dexec.mainClass="com.example.demo.SimpleCrawlerTest" -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 爬虫测试失败" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 故障排除建议：" -ForegroundColor Yellow
    Write-Host "  1. 检查Chrome浏览器是否已安装" -ForegroundColor White
    Write-Host "  2. 检查网络连接" -ForegroundColor White
    Write-Host "  3. 确认目标网站可访问" -ForegroundColor White
    Write-Host "  4. 查看详细错误日志" -ForegroundColor White
} else {
    Write-Host "✅ 爬虫测试完成" -ForegroundColor Green
}

Write-Host ""
Read-Host "按回车键退出" 