@echo off
chcp 65001 >nul
title PIM 数据采集守护程序 - 安装器

echo ========================================
echo   PIM 数据采集守护程序 - 安装器
echo ========================================
echo.

if not exist "%~dp0PimDaemon\Pim.Client.App.exe" (
    echo [错误] 未找到 PimDaemon\Pim.Client.App.exe
    echo 请先完整解压 release 安装包后再运行安装。
    pause
    exit /b 1
)

powershell -ExecutionPolicy Bypass -File "%~dp0install.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [错误] 安装失败。请尝试右键 install.ps1，选择“使用 PowerShell 运行”。
    pause
    exit /b %ERRORLEVEL%
)

pause
