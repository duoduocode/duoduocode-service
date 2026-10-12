# ECS 后端部署 / 回滚脚本（两级闸门的"部署"环节执行器）
#
# 用法：
#   powershell -File scripts\deploy_test.ps1                    # 部署测试环境（8080）
#   powershell -File scripts\deploy_test.ps1 -Target prod       # 部署生产环境（8081，自动先备份 RDS）
#   powershell -File scripts\deploy_test.ps1 -Rollback          # 回滚当前 Target 到上一版 jar
#   powershell -File scripts\deploy_test.ps1 -SkipPackage       # 跳过本地打包（复用已有 jar，调试用）
#
# 一次性前置配置（首次使用前完成一次即可）：
#   1. 本机 ssh 免密登录 ECS：ssh-keygen 生成密钥后把公钥追加到 ECS 的 ~/.ssh/authorized_keys
#   2. 环境变量：$env:ECS_HOST="114.55.116.21"、$env:ECS_SSH_KEY="密钥路径"（ECS_USER 缺省 root）
#   3. ECS 上创建 systemd 服务（模板见本文件尾部注释）与环境文件
#      /opt/duoduocode/{test|prod}/{test|prod}.env：
#        SPRING_PROFILES_ACTIVE=test|prod
#        DB_PASSWORD=<对应 RDS 密码>
#        WX_SECRET=<小程序密钥>
#        JWT_SECRET=<生产级密钥>
#
# 部署纪律（AGENTS.md）：部署由用户口令触发（「push」→test，「提交」→prod）；本脚本只做部署不做合并。
param(
  [ValidateSet("test", "prod")]
  [string]$Target = "test",
  [switch]$Rollback,
  [switch]$SkipPackage,
  [string]$EcsHost = $env:ECS_HOST,
  [string]$EcsUser = $(if ($env:ECS_USER) { $env:ECS_USER } else { "root" }),
  [string]$EcsKey = $env:ECS_SSH_KEY
)
$ErrorActionPreference = "Stop"
$root = "d:\work\duoduocode_account"
$jar = Join-Path $root "duoduocode-service\target\duoduocode-service-1.0.0.jar"
$py = Join-Path $root ".tools\python312\python.exe"

# 目标环境映射
$map = @{
  test = @{ Remote = "/opt/duoduocode/test";  Service = "duoduocode-test";  Health = "https://duoduocode.cn/test/api";  Smoke = "-m smoke" }
  prod = @{ Remote = "/opt/duoduocode/prod";  Service = "duoduocode-prod";  Health = "https://duoduocode.cn/prod/api";  Smoke = "-m prodsmoke --env prod" }
}
$cfg = $map[$Target]

if (-not $EcsHost) { throw "缺少 ECS_HOST 环境变量（或在参数 -EcsHost 指定）" }
$sshArgs = @()
if ($EcsKey) { $sshArgs += @("-i", $EcsKey) }
function Remote($cmd) { ssh @sshArgs "$EcsUser@$EcsHost" $cmd }

# ── 回滚模式 ──
if ($Rollback) {
  Write-Host "[回滚] $Target 恢复上一版 jar 并重启 ..."
  Remote "cd $($cfg.Remote) && if [ -f app.jar.bak ]; then cp app.jar app.jar.bad && mv app.jar.bak app.jar && systemctl restart $($cfg.Service) && echo ROLLED_BACK; else echo NO_BACKUP; exit 1; fi"
  Start-Sleep -Seconds 5
  Write-Host "[回滚] 冒烟验证 ..."
  Push-Location (Join-Path $root "test")
  & $py -m pytest $cfg.Smoke.Split(" ") 2>&1 | Select-Object -Last 5
  Pop-Location
  exit $LASTEXITCODE
}

# ── 正常部署 ──
if (-not $SkipPackage) {
  Write-Host "[1/5] 本地打包 mvn package -DskipTests ..."
  $env:JAVA_HOME = (Get-ChildItem (Join-Path $root ".tools\jdk") -Directory | Select-Object -First 1).FullName
  $env:Path = "$env:JAVA_HOME\bin;$(Join-Path $root '.tools\apache-maven-3.9.9\bin');$env:Path"
  Push-Location (Join-Path $root "duoduocode-service")
  mvn package -DskipTests -q
  if ($LASTEXITCODE -ne 0) { Pop-Location; throw "打包失败" }
  Pop-Location
  if (-not (Test-Path $jar)) { throw "jar 未生成：$jar" }
} else {
  if (-not (Test-Path $jar)) { throw "jar 不存在：$jar" }
}
Write-Host "  jar: $jar ($([math]::Round((Get-Item $jar).Length/1MB,1)) MB)"

if ($Target -eq "prod") {
  Write-Host "[2/5] 生产库备份（mysqldump → ECS 本地文件）..."
  Remote "cd $($cfg.Remote) && source $($cfg.Remote)/prod.env >/dev/null 2>&1; mysqldump -h`$DB_HOST -u`$DB_USER -p`$DB_PASSWORD `$DB_NAME | gzip > backup/db_`$(date +%Y%m%d_%H%M%S).sql.gz && echo BACKUP_OK"
  if ($LASTEXITCODE -ne 0) { throw "生产库备份失败，中止部署" }
} else {
  Write-Host "[2/5] 测试环境无需备份，跳过"
}

Write-Host "[3/5] 上传 jar 并重启服务 ..."
scp @sshArgs $jar "$($EcsUser)@$($EcsHost):$($cfg.Remote)/app.jar.new"
if ($LASTEXITCODE -ne 0) { throw "scp 上传失败" }
Remote "cd $($cfg.Remote) && cp app.jar app.jar.bak 2>/dev/null; mv app.jar.new app.jar && systemctl restart $($cfg.Service) && echo RESTARTED"

Write-Host "[4/5] 健康探测（等待服务就绪）..."
$ready = $false
foreach ($i in 1..12) {
  Start-Sleep -Seconds 5
  try {
    # 无 token 期望 401 = 服务存活且 JWT 生效；连接被拒/超时则继续等
    $r = Invoke-WebRequest -Uri "$($cfg.Health)/v1/user/profile" -Method GET -UseBasicParsing -ErrorAction SilentlyContinue
    if ($r.StatusCode -eq 401 -or $r.StatusCode -eq 200) { $ready = $true; break }
    if ($r.StatusCode) { $ready = $true; break }  # 其他 HTTP 响应也说明服务已起
  } catch {
    $code = $_.Exception.Response.StatusCode.value__
    if ($code -eq 401) { $ready = $true; break }  # 401 走异常分支（PS5.1 4xx 抛异常）
    Write-Host "  等待中 ... ($i)"
  }
}
if (-not $ready) { Write-Host "[FAIL] 服务 60s 内未就绪，请上 ECS 查 journalctl -u $($cfg.Service)" -ForegroundColor Red; exit 1 }
Write-Host "  服务就绪"

Write-Host "[5/5] 冒烟验证（$($cfg.Smoke)）..."
Push-Location (Join-Path $root "test")
& $py -m pytest $cfg.Smoke.Split(" ") 2>&1 | Select-Object -Last 6
Pop-Location
exit $LASTEXITCODE

<#
ECS systemd 服务模板（duoduocode-test.service，prod 同理改名换路径）：

[Unit]
Description=duoduocode backend (test)
After=network.target

[Service]
WorkingDirectory=/opt/duoduocode/test
EnvironmentFile=/opt/duoduocode/test/test.env
ExecStart=/usr/bin/java -javaagent:/opt/duoduocode/jacocoagent.jar=output=file,append=false,destfile=/opt/duoduocode/test/jacoco-exec/interface.exec,includes=com.duoduocode.* -jar /opt/duoduocode/test/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target

说明：
- JaCoCo agent 用 output=file：每次重启 append=false 覆盖写，pytest 打 ECS 后由
  preflight.ps1 -Full 通过 ECS_JACOCO_REMOTE 拉取该文件做四维覆盖率门禁。
- prod 服务如不需要接口覆盖率，可去掉 -javaagent 参数。
#>
