# Build script for backend-java.
# Runs without extra privileges (works in restricted / sandboxed environments).
#
# Why: this machine's Maven localRepository is D:\develop\apache-maven-3.9.4\mvn_repo,
#      which may be read-only. This script uses a writable copy instead (under system TEMP),
#      creating it on first run (about 270MB, a few seconds) and reusing it afterwards.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File tools\build.ps1
#   powershell -ExecutionPolicy Bypass -File tools\build.ps1 -SkipTests
#   powershell -ExecutionPolicy Bypass -File tools\build.ps1 -Goals "test"
#
# The script always adds -Psandbox (which excludes Mockito). Tests in this project do not
# use Mockito; the profile only makes the suite runnable where Java agents are unavailable.

param(
    [string]$Goals = "clean package",
    [switch]$SkipTests,
    [string]$MavenRepo,
    [string]$JavaHome
)

$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
$sourceRepo = "D:\develop\apache-maven-3.9.4\mvn_repo"

if (-not $MavenRepo) {
    $MavenRepo = Join-Path ([System.IO.Path]::GetTempPath()) "mvn-repo"
}
if (-not $JavaHome) {
    $JavaHome = "D:\develop\corretto-21.0.12"
}

# 1. prepare a writable local repository
$sentinel = Join-Path $MavenRepo "org\springframework\boot\spring-boot-starter-parent\3.4.4"
if (-not (Test-Path $sentinel)) {
    if (-not (Test-Path $sourceRepo)) {
        throw "source repository not found: $sourceRepo (use -MavenRepo to point at an existing one)"
    }
    Write-Host "[build] first run, copying local repository to $MavenRepo" -ForegroundColor Cyan
    New-Item -ItemType Directory -Force -Path $MavenRepo | Out-Null
    Copy-Item "$sourceRepo\*" $MavenRepo -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "[build] copy done" -ForegroundColor Cyan
} else {
    Write-Host "[build] reusing local repository copy: $MavenRepo" -ForegroundColor DarkGray
}

# 2. compose maven arguments
$mvnArgs = @("-B", "-o", "-Psandbox", "-Dmaven.repo.local=$MavenRepo", "-f", (Join-Path $projectDir "pom.xml"))
if ($SkipTests) { $mvnArgs += "-DskipTests" }
$mvnArgs += $Goals.Split(" ")

$env:JAVA_HOME = $JavaHome
Write-Host "[build] JAVA_HOME=$JavaHome" -ForegroundColor DarkGray
Write-Host "[build] mvn $($mvnArgs -join ' ')" -ForegroundColor DarkGray

# 说明：Windows PowerShell 5.1 会把原生命令（mvn.cmd）写入 stderr 的内容包装成
# NativeCommandError 错误记录。这里在调用期间把 ErrorActionPreference 降为
# SilentlyContinue，并用 "2>&1" 合并错误流后再重定向，避免脚本被误判而中断。
$log = Join-Path $projectDir ".build.log"

Write-Host "[build] mvn $($mvnArgs -join ' ')" -ForegroundColor DarkGray
$ErrorActionPreference = "SilentlyContinue"
& mvn @mvnArgs 2>&1 | Out-File -FilePath $log -Encoding utf8
$exit = $LASTEXITCODE
$ErrorActionPreference = "Stop"

Get-Content $log | Select-String -Pattern "BUILD SUCCESS|BUILD FAILURE|Tests run:.*Failures|Building jar|^\[ERROR\]" |
    Select-Object -First 20 | ForEach-Object { $_.Line }

if ($exit -ne 0) {
    Write-Host "[build] FAILED - full log: $log" -ForegroundColor Red
    exit $exit
}

$jar = Join-Path $projectDir "target\csu-takeout.jar"
if (Test-Path $jar) {
    $size = [math]::Round((Get-Item $jar).Length / 1MB, 1)
    Write-Host "[build] OK -> $jar ($size MB)" -ForegroundColor Green
}
exit 0
