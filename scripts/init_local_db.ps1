# 本地开发库初始化：重建 duoduocode_dev + 全量 DDL + 种子数据
# 用法：
#   .\scripts\init_local_db.ps1                      # 密码取 $env:DB_PASSWORD，缺省 duoduo123
#   .\scripts\init_local_db.ps1 -Password xxx        # 显式指定 root 密码
# 注意：会 DROP 现有 duoduocode_dev 库（本地开发库，重建成本低；生产数据在 RDS，不受影响）
param(
    [string]$Password = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'duoduo123' }),
    [string]$Mysql = 'mysql'
)

$ErrorActionPreference = 'Stop'
$res = Join-Path $PSScriptRoot '..\src\main\resources'

# mysql 客户端存在性检查
if (-not (Get-Command $Mysql -ErrorAction SilentlyContinue)) {
    # 常见安装路径兜底
    foreach ($p in @('C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
                     'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe')) {
        if (Test-Path $p) { $Mysql = $p; break }
    }
}
if (-not (Get-Command $Mysql -ErrorAction SilentlyContinue)) {
    Write-Error "找不到 mysql 客户端，请安装 MySQL 或用 -Mysql 指定路径"
}

Write-Host '== 1/3 重建数据库 duoduocode_dev =='
& $Mysql -h 127.0.0.1 -uroot "-p$Password" -e "DROP DATABASE IF EXISTS duoduocode_dev; CREATE DATABASE duoduocode_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if ($LASTEXITCODE -ne 0) { Write-Error "重建失败（检查 root 密码/服务是否启动）" }

Write-Host '== 2/3 导入全量 DDL（schema.sql） =='
Get-Content (Join-Path $res 'schema.sql') -Encoding UTF8 | & $Mysql -h 127.0.0.1 -uroot "-p$Password" duoduocode_dev
if ($LASTEXITCODE -ne 0) { Write-Error "schema.sql 导入失败" }

Write-Host '== 3/3 导入种子数据（init_category_data.sql） =='
Get-Content (Join-Path $res 'db\init_category_data.sql') -Encoding UTF8 | & $Mysql -h 127.0.0.1 -uroot "-p$Password" duoduocode_dev
if ($LASTEXITCODE -ne 0) { Write-Error "种子数据导入失败" }

Write-Host ''
Write-Host '[完成] 本地库就绪。启动后端: mvn spring-boot:run -Dspring-boot.run.profiles=local（缺省 profile 即 local）'
