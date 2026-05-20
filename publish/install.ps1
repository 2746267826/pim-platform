# PIM 数据采集守护程序安装器
param(
    [switch]$Uninstall,
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"
$AppName = "PIM 数据采集守护程序"
$AppDir = Join-Path $env:LocalAppData "PIM"
$ExePath = Join-Path $AppDir "Pim.Client.App.exe"
$AutoStartKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Run"
$AutoStartName = "PIM"
$StartMenuDir = Join-Path $env:AppData "Microsoft\Windows\Start Menu\Programs\PIM"

function Stop-PimDaemon {
    $proc = Get-Process -Name "Pim.Client.App" -ErrorAction SilentlyContinue
    if ($proc) {
        Write-Host "正在停止已运行的守护程序..."
        $proc | Stop-Process -Force
        Start-Sleep -Seconds 1
    }
}

if ($Uninstall) {
    Write-Host "=== 卸载 $AppName ===" -ForegroundColor Yellow
    Stop-PimDaemon

    Remove-ItemProperty -Path $AutoStartKey -Name $AutoStartName -ErrorAction SilentlyContinue
    Write-Host "已移除开机自启动项。"

    if (Test-Path $StartMenuDir) {
        Remove-Item -LiteralPath $StartMenuDir -Recurse -Force
        Write-Host "已移除开始菜单快捷方式。"
    }

    if (Test-Path $AppDir) {
        Remove-Item -LiteralPath $AppDir -Recurse -Force
        Write-Host "已移除应用程序目录。"
    }

    Write-Host "卸载完成。" -ForegroundColor Green
    exit 0
}

Write-Host "=== 安装 $AppName ===" -ForegroundColor Cyan
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$SourceDir = Join-Path $ScriptDir "PimDaemon"

if (-not (Test-Path (Join-Path $SourceDir "Pim.Client.App.exe"))) {
    throw "未找到 PimDaemon\Pim.Client.App.exe，请先完整解压 release 安装包。"
}

Stop-PimDaemon

Write-Host "[1/5] 正在创建安装目录..."
New-Item -ItemType Directory -Force -Path $AppDir | Out-Null

Write-Host "[2/5] 正在复制应用程序文件..."
Copy-Item -Path (Join-Path $SourceDir "*") -Destination $AppDir -Recurse -Force
Copy-Item $MyInvocation.MyCommand.Path -Destination (Join-Path $AppDir "uninstall.ps1") -Force

Write-Host "[3/5] 正在启用登录后自启动..."
$runValue = "`"$ExePath`""
Set-ItemProperty -Path $AutoStartKey -Name $AutoStartName -Value $runValue

Write-Host "[4/5] 正在创建开始菜单快捷方式..."
New-Item -ItemType Directory -Force -Path $StartMenuDir | Out-Null
$WScriptShell = New-Object -ComObject WScript.Shell

$StatusShortcut = $WScriptShell.CreateShortcut((Join-Path $StartMenuDir "PIM 数据采集守护程序.lnk"))
$StatusShortcut.TargetPath = $ExePath
$StatusShortcut.Description = "启动 PIM 数据采集守护程序"
$StatusShortcut.Save()

$UninstallShortcut = $WScriptShell.CreateShortcut((Join-Path $StartMenuDir "卸载 PIM 数据采集守护程序.lnk"))
$UninstallShortcut.TargetPath = "powershell.exe"
$UninstallShortcut.Arguments = "-ExecutionPolicy Bypass -File `"$AppDir\uninstall.ps1`" -Uninstall"
$UninstallShortcut.Description = "卸载 PIM 数据采集守护程序"
$UninstallShortcut.Save()

Write-Host "[5/5] 正在启动守护程序..."
if (-not $NoLaunch) {
    Start-Process -FilePath $ExePath -WindowStyle Hidden
}

Write-Host ""
Write-Host "安装完成。" -ForegroundColor Green
Write-Host "应用目录：$AppDir"
Write-Host "自启动：已通过 HKCU Run 项 '$AutoStartName' 启用"
Write-Host "开始菜单：$StartMenuDir"
