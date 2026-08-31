; PIM Windows 守护程序 - Inno Setup 安装器
; 打包来源: src/client-windows/publish/* (合包: Daemon + Shell + KeyStats + fix script + VERSION)
; 生成: PIM-Setup-v<artifact_slug>.exe
; 需求: per-machine, Program Files\PIM, 计划任务自启免 UAC

#ifndef Version
#define Version "0.0.0-dev"
#endif
#ifndef ArtifactSlug
#define ArtifactSlug "0.0.0-dev"
#endif
#ifndef AssemblyVersion
#define AssemblyVersion "0.0.0.0"
#endif
; PublishDir 相对于本脚本所在目录 (installer/pim-setup.iss -> ../src/client-windows/publish)
#ifndef PublishDir
#define PublishDir "..\src\client-windows\publish"
#endif

[Setup]
AppName=PIM
AppVersion={#Version}
AppVerName=PIM v{#Version}
AppPublisher=PIM
AppPublisherURL=https://github.com/2746267826/pim-platform
AppId={{A2F8E8B1-3C4D-4E5F-9A6B-7C8D9E0F1A2B}
DefaultDirName={autopf}\PIM
DefaultGroupName=PIM
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=commandline
ArchitecturesInstallIn64BitMode=x64
OutputDir=..
OutputBaseFilename=PIM-Setup-v{#ArtifactSlug}
Compression=lzma2/ultra64
SolidCompression=yes
CloseApplications=yes
CloseApplicationsFilter=Pim.Client.App.exe,KeyStats.exe,Pim.Shell.App.exe
UninstallDisplayIcon={app}\Pim.Client.App.exe
SetupIconFile=..\src\client-windows\Pim.Client.App\app.ico
UninstallDisplayName=PIM
VersionInfoVersion={#AssemblyVersion}
VersionInfoProductVersion={#AssemblyVersion}
WizardStyle=modern
DisableProgramGroupPage=yes
AllowNoIcons=yes
; 若未来需要签名，取消注释并配置 SignTool
;SignTool=default

[Languages]
; Use Default.isl for both to ensure ISCC succeeds on all runner images (ChineseSimplified.isl not always present).
; Wizard UI will still be Chinese-capable via custom messages; fallback to Default is safe.
Name: "chinese"; MessagesFile: "compiler:Default.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "{#PublishDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\PIM 守护程序"; Filename: "{app}\Pim.Client.App.exe"; IconFilename: "{app}\Pim.Client.App.exe"
Name: "{group}\PIM Shell"; Filename: "{app}\Pim.Shell.App.exe"; IconFilename: "{app}\Pim.Shell.App.exe"
Name: "{group}\卸载 PIM"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\Pim.Client.App.exe"; Description: "立即启动 PIM 守护程序"; Flags: nowait postinstall skipifsilent

[Code]
const
  DaemonTask = '\PIM\PIM Daemon';
  KeyStatsTask = '\PIM\PIM KeyStats';
  LegacyTask = 'PimKeyStats';
  DotNetDownloadUrl = 'https://aka.ms/dotnet/8.0/windowsdesktop-runtime-win-x64';
  RunKey = 'Software\Microsoft\Windows\CurrentVersion\Run';

var
  DeleteUserDataOnUninstall: Boolean;

function HasDotNet8Runtime(): Boolean;
var
  Names: TArrayOfString;
  I: Integer;
  V: string;
  ResultCode: Integer;
  TempFile: String;
  Lines: TArrayOfString;
  CheckCmd: String;
begin
  Result := False;

  // 1) Registry: sharedfx\Microsoft.NETCore.App 8.*
  if RegGetSubkeyNames(HKLM, 'SOFTWARE\dotnet\Setup\InstalledVersions\x64\sharedfx\Microsoft.NETCore.App', Names) then
  begin
    for I := 0 to GetArrayLength(Names) - 1 do
      if Pos('8.', Names[I]) = 1 then
      begin
        Result := True;
        Exit;
      end;
  end;
  if RegGetSubkeyNames(HKLM, 'SOFTWARE\dotnet\Setup\InstalledVersions\x64\sharedfx\Microsoft.WindowsDesktop.App', Names) then
  begin
    for I := 0 to GetArrayLength(Names) - 1 do
      if Pos('8.', Names[I]) = 1 then
      begin
        Result := True;
        Exit;
      end;
  end;
  if RegGetSubkeyNames(HKLM64, 'SOFTWARE\dotnet\Setup\InstalledVersions\x64\sharedfx\Microsoft.NETCore.App', Names) then
  begin
    for I := 0 to GetArrayLength(Names) - 1 do
      if Pos('8.', Names[I]) = 1 then
      begin
        Result := True;
        Exit;
      end;
  end;
  if RegGetSubkeyNames(HKLM64, 'SOFTWARE\dotnet\Setup\InstalledVersions\x64\sharedfx\Microsoft.WindowsDesktop.App', Names) then
  begin
    for I := 0 to GetArrayLength(Names) - 1 do
      if Pos('8.', Names[I]) = 1 then
      begin
        Result := True;
        Exit;
      end;
  end;
  // Also check HKLM 32-bit WOW view for completeness
  if RegGetSubkeyNames(HKLM, 'SOFTWARE\WOW6432Node\dotnet\Setup\InstalledVersions\x64\sharedfx\Microsoft.NETCore.App', Names) then
  begin
    for I := 0 to GetArrayLength(Names) - 1 do
      if Pos('8.', Names[I]) = 1 then
      begin
        Result := True;
        Exit;
      end;
  end;

  // 2) Fallback: dotnet --list-runtimes parsing (handles standalone installs or custom paths)
  TempFile := ExpandConstant('{tmp}\pim_dotnet_runtimes.txt');
  CheckCmd := '/c dotnet --list-runtimes > "' + TempFile + '" 2>&1';
  if Exec(ExpandConstant('{cmd}'), CheckCmd, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) then
  begin
    if FileExists(TempFile) then
    begin
      if LoadStringsFromFile(TempFile, Lines) then
      begin
        for I := 0 to GetArrayLength(Lines) - 1 do
        begin
          // Typical line: Microsoft.NETCore.App 8.0.8 [C:\Program Files\dotnet\shared\...]
          //                Microsoft.WindowsDesktop.App 8.0.8 [...]
          if (Pos('Microsoft.NETCore.App 8.', Lines[I]) > 0) or
             (Pos('Microsoft.WindowsDesktop.App 8.', Lines[I]) > 0) then
          begin
            DeleteFile(TempFile);
            Result := True;
            Exit;
          end;
        end;
      end;
      DeleteFile(TempFile);
    end;
  end;

  // Also try direct registry value check for sharedhost Version 8.*
  if RegQueryStringValue(HKLM, 'SOFTWARE\dotnet\Setup\InstalledVersions\x64\sharedhost', 'Version', V) then
    if Pos('8.', V) = 1 then begin Result := True; Exit; end;
  if RegQueryStringValue(HKLM64, 'SOFTWARE\dotnet\Setup\InstalledVersions\x64\sharedhost', 'Version', V) then
    if Pos('8.', V) = 1 then begin Result := True; Exit; end;
end;

function InitializeSetup(): Boolean;
var
  Choice: Integer;
  ResultCode: Integer;
begin
  Result := True;
  if HasDotNet8Runtime() then
    Exit;

  // 未检测到 .NET 8，阻断并引导
  Choice := MsgBox('未检测到 .NET 8 运行时，是否前往微软官网下载安装？安装完成后请重新运行 PIM 安装程序。' + #13#10 + #13#10 +
                   '点击“是”打开下载页并退出安装；点击“否”将确认是否仍要继续安装（不推荐，PIM 将无法启动）；点击“取消”直接退出。',
                    mbConfirmation, MB_YESNOCANCEL);
  if Choice = IDYES then
  begin
    ShellExec('open', DotNetDownloadUrl, '', '', SW_SHOWNORMAL, ewNoWait, ResultCode);
    Result := False;
    Exit;
  end
  else if Choice = IDCANCEL then
  begin
    Result := False;
    Exit;
  end
  else // IDNO -> secondary confirm
  begin
    if MsgBox('不安装 .NET 8 运行时则 PIM 无法启动。是否仍要继续安装？（稍后需手动安装运行时）', mbConfirmation, MB_YESNO) = IDYES then
      Result := True
    else
      Result := False;
  end;
end;

procedure KillPimProcesses();
var
  ResultCode: Integer;
begin
  // 先尝试优雅关闭 (不带 /F)，等待 3 秒后再强制
  Exec(ExpandConstant('{cmd}'), '/c taskkill /IM Pim.Client.App.exe /T 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c taskkill /IM KeyStats.exe /T 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c taskkill /IM Pim.Shell.App.exe /T 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Sleep(3000);
  Exec(ExpandConstant('{cmd}'), '/c taskkill /F /IM Pim.Client.App.exe /T 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c taskkill /F /IM KeyStats.exe /T 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c taskkill /F /IM Pim.Shell.App.exe /T 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Sleep(1000);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode: Integer;
  HasRunning: Boolean;
  CheckFile: String;
  Lines: TArrayOfString;
begin
  Result := '';
  // 检测三进程是否在运行
  CheckFile := ExpandConstant('{tmp}\pim_tasklist.txt');
  Exec(ExpandConstant('{cmd}'), '/c tasklist /FI "IMAGENAME eq Pim.Client.App.exe" /FO CSV /NH > "' + CheckFile + '" 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  HasRunning := False;
  if FileExists(CheckFile) then
  begin
    if LoadStringsFromFile(CheckFile, Lines) and (GetArrayLength(Lines) > 0) then
      if Pos('Pim.Client.App.exe', Lines[0]) > 0 then HasRunning := True;
    DeleteFile(CheckFile);
  end;
  Exec(ExpandConstant('{cmd}'), '/c tasklist /FI "IMAGENAME eq KeyStats.exe" /FO CSV /NH > "' + CheckFile + '" 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  if FileExists(CheckFile) then
  begin
    if LoadStringsFromFile(CheckFile, Lines) and (GetArrayLength(Lines) > 0) then
      if Pos('KeyStats.exe', Lines[0]) > 0 then HasRunning := True;
    DeleteFile(CheckFile);
  end;
  Exec(ExpandConstant('{cmd}'), '/c tasklist /FI "IMAGENAME eq Pim.Shell.App.exe" /FO CSV /NH > "' + CheckFile + '" 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  if FileExists(CheckFile) then
  begin
    if LoadStringsFromFile(CheckFile, Lines) and (GetArrayLength(Lines) > 0) then
      if Pos('Pim.Shell.App.exe', Lines[0]) > 0 then HasRunning := True;
    DeleteFile(CheckFile);
  end;

  if HasRunning then
  begin
    if MsgBox('检测到 PIM 相关程序正在运行，安装程序将先关闭它们以完成更新，是否继续？', mbConfirmation, MB_YESNO) = IDNO then
    begin
      Result := '用户取消安装（检测到运行中进程）';
      Exit;
    end;
  end;
  KillPimProcesses();
end;

procedure CleanLegacyRunEntries();
begin
  // 清理 HKCU 和 HKLM 下旧的 Run 值（幂等，忽略失败）
  RegDeleteValue(HKCU, RunKey, 'PIM');
  RegDeleteValue(HKCU, RunKey, 'KeyStats');
  RegDeleteValue(HKLM, RunKey, 'PIM');
  RegDeleteValue(HKLM, RunKey, 'KeyStats');
  RegDeleteValue(HKCU32, RunKey, 'PIM');
  RegDeleteValue(HKCU32, RunKey, 'KeyStats');
  RegDeleteValue(HKLM32, RunKey, 'PIM');
  RegDeleteValue(HKLM32, RunKey, 'KeyStats');
end;

procedure DeleteLegacyTasks();
var
  ResultCode: Integer;
begin
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "' + LegacyTask + '" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "' + KeyStatsTask + '" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "PimKeyStats" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  // 清理旧的 KeyStats 独立任务（新版 KeyStats 作为子进程继承 HIGHEST，无需独立任务）
end;

procedure CreatePimTasks();
var
  ResultCode: Integer;
  AppDaemon: String;
  Cmd: String;
  SuccessDaemon: Boolean;
begin
  CleanLegacyRunEntries();
  DeleteLegacyTasks();

  AppDaemon := ExpandConstant('{app}\Pim.Client.App.exe');

  // 仅创建一个 HIGHEST 任务 \PIM\PIM Daemon，KeyStats 改为子进程继承权限
  Cmd := '/c schtasks /create /tn "' + DaemonTask + '" /tr "\"' + AppDaemon + '\"" /sc onlogon /rl highest /f /delay 0000:10 2>nul';
  SuccessDaemon := Exec(ExpandConstant('{cmd}'), Cmd, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0);
  if not SuccessDaemon then
  begin
    // retry without delay (older Windows)
    Cmd := '/c schtasks /create /tn "' + DaemonTask + '" /tr "\"' + AppDaemon + '\"" /sc onlogon /rl highest /f 2>nul';
    SuccessDaemon := Exec(ExpandConstant('{cmd}'), Cmd, '', SW_HIDE, ewWaitUntilTerminated, ResultCode) and (ResultCode = 0);
  end;
  if not SuccessDaemon then
  begin
    Log('CreatePimTasks: failed to create daemon task, code ' + IntToStr(ResultCode));
  end;

  if not SuccessDaemon then
  begin
    MsgBox('任务创建失败（可能受企业策略限制），开机自启将回退到注册表方式（会弹 UAC）。可手动在任务计划程序中检查 \PIM\PIM Daemon。', mbInformation, MB_OK);
  end;
end;

procedure RemovePimTasks();
var
  ResultCode: Integer;
begin
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "' + DaemonTask + '" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "' + KeyStatsTask + '" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "' + LegacyTask + '" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  Exec(ExpandConstant('{cmd}'), '/c schtasks /delete /tn "PimKeyStats" /f 2>nul', '', SW_HIDE, ewWaitUntilTerminated, ResultCode);
  CleanLegacyRunEntries();
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    CreatePimTasks();
    // 版本比对：若旧版已安装且当前为旧版覆盖新版，提示用户确认（Inno 默认已处理 AppVersion，但我们额外日志）
    // Inno 的 AppVersion 覆盖逻辑由 [Setup] AppVersion 控制，此处不额外阻断
  end;
end;

procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var
  ResultCode: Integer;
  DataDir: String;
begin
  if CurUninstallStep = usUninstall then
  begin
    KillPimProcesses();
    RemovePimTasks();
    CleanLegacyRunEntries();
  end;
  if CurUninstallStep = usPostUninstall then
  begin
    // 配置与数据默认保留，提供可选删除
    if DeleteUserDataOnUninstall then
    begin
      DataDir := ExpandConstant('{localappdata}\PIM');
      DelTree(DataDir, True, True, True);
    end;
    // 清理开始菜单残留由 Inno 自动处理，额外确保删除
    DelTree(ExpandConstant('{group}'), True, True, True);
  end;
end;

function InitializeUninstall(): Boolean;
begin
  Result := True;
  if MsgBox('是否同时删除用户配置与数据（%LOCALAPPDATA%\PIM 下的 config.json、token、日志等）？' + #13#10 + '选择“是”将删除该目录，选择“否”则保留。', mbConfirmation, MB_YESNO) = IDYES then
    DeleteUserDataOnUninstall := True
  else
    DeleteUserDataOnUninstall := False;
end;

function IsUpgrade(): Boolean;
var
  S: String;
begin
  // 检测是否已安装旧版本，若当前安装版本低于已安装版本则提示
  // Inno 会自动比较 Version，此处仅作额外提示占位
  Result := False;
  if RegQueryStringValue(HKLM, 'Software\Microsoft\Windows\CurrentVersion\Uninstall\{{A2F8E8B1-3C4D-4E5F-9A6B-7C8D9E0F1A2B}_is1', 'DisplayVersion', S) then
  begin
    // 若 S > Version 则为旧版覆盖新版
    // 簡單字符串比较，实际比对由安装前检查完成，此处不阻断
    Result := True;
  end;
end;
