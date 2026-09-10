using System.Diagnostics;
using System.IO;
using System.Security.AccessControl;
using System.Security.Principal;
using System.Threading;

namespace Pim.Client.App.Services;

/// <summary>
/// 单实例互斥守护服务：支持 Windows Global\ 与 Local\ 双层互斥及跨权限（HIGHEST 计划任务 vs 普通用户）探测。
/// </summary>
internal static class SingleInstanceGuard
{
    internal const string DefaultGlobalMutexName = @"Global\PIM_Daemon_SingleInstance";
    internal const string DefaultLocalMutexName = @"Local\PIM_Daemon_SingleInstance";

    // 静态字段持有进程生命周期，防止 GC 回收句柄
    private static Mutex? _heldMutex;

    /// <summary>
    /// 尝试获取单实例互斥。成功获取返回 true，若已有实例运行返回 false。
    /// </summary>
    internal static bool TryAcquire(string globalName = DefaultGlobalMutexName, string localName = DefaultLocalMutexName)
    {
        // 1. 探测 Global 互斥体
        try
        {
            if (Mutex.TryOpenExisting(globalName, out var existingGlobal))
            {
                existingGlobal.Dispose();
                return false;
            }
        }
        catch (UnauthorizedAccessException)
        {
            // 内核对象已存在但调用方无权打开，说明确有高权限实例运行
            return false;
        }
        catch (Exception ex)
        {
            BootstrapLog.Write($"Global mutex probe error ({ex.Message})");
        }

        // 2. 探测 Local 互斥体
        try
        {
            if (Mutex.TryOpenExisting(localName, out var existingLocal))
            {
                existingLocal.Dispose();
                return false;
            }
        }
        catch (UnauthorizedAccessException)
        {
            return false;
        }
        catch (Exception ex)
        {
            BootstrapLog.Write($"Local mutex probe error ({ex.Message})");
        }

        // 3. 尝试创建 Global 互斥体并显式赋予 WorldSid 访问权限（便于非提权实例探测与退出）
        try
        {
            bool createdNew;
            if (OperatingSystem.IsWindows())
            {
                var security = new MutexSecurity();
                security.AddAccessRule(new MutexAccessRule(
                    new SecurityIdentifier(WellKnownSidType.WorldSid, null),
                    MutexRights.FullControl,
                    AccessControlType.Allow));
                _heldMutex = MutexAcl.Create(true, globalName, out createdNew, security);
            }
            else
            {
                _heldMutex = new Mutex(true, globalName, out createdNew);
            }

            if (!createdNew)
            {
                return false;
            }

            BootstrapLog.Write("Mutex acquired (Global)");
            return true;
        }
        catch (UnauthorizedAccessException)
        {
            // 普通权限进程无 SeCreateGlobalPrivilege，无法在 Global 命名空间创建，降级尝试 Local
            BootstrapLog.Write("Global mutex creation restricted; falling back to Local");
        }
        catch (Exception ex)
        {
            BootstrapLog.Write($"Global mutex creation failed ({ex.Message}); falling back to Local");
        }

        // 4. 降级创建 Local 互斥体
        try
        {
            _heldMutex = new Mutex(true, localName, out bool createdNew);
            if (!createdNew)
            {
                return false;
            }

            BootstrapLog.Write("Mutex acquired (Local)");
            return true;
        }
        catch (Exception ex)
        {
            BootstrapLog.Write($"Local mutex creation failed ({ex.Message}); continuing without single-instance guard");
            return true;
        }
    }

    /// <summary>
    /// 释放互斥体持有（供进程退出或单元测试重置）。
    /// </summary>
    internal static void Release()
    {
        var m = Interlocked.Exchange(ref _heldMutex, null);
        if (m is null) return;
        try
        {
            m.ReleaseMutex();
        }
        catch
        {
            // 忽略未持有状态下的异常
        }
        finally
        {
            m.Dispose();
        }
    }
}
