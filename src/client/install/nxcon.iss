; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
AppName=NetXMS Management Console
AppVerName=NetXMS Management Console 1.1.2
AppVersion=1.1.2
AppPublisher=NetXMS Team
AppPublisherURL=http://www.netxms.org
AppSupportURL=http://www.netxms.org
AppUpdatesURL=http://www.netxms.org
DefaultDirName={pf}\NetXMS Management Console
DefaultGroupName=NetXMS
AllowNoIcons=yes
LicenseFile=..\..\..\GPL.txt
OutputBaseFilename=netxms-console-1.1.2
Compression=lzma
SolidCompression=yes
LanguageDetectionMethod=none
PrivilegesRequired=admin

[Components]
Name: "base"; Description: "Base Files"; Types: full compact custom; Flags: fixed
Name: "console"; Description: "Administrator's Console"; Types: full compact
Name: "tools"; Description: "Command Line Tools"; Types: full
Name: "pdb"; Description: "Install PDB files for selected components"; Types: full custom

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Common files
Source: "..\..\..\ChangeLog"; DestDir: "{app}\doc"; Flags: ignoreversion; Components: base
Source: "..\..\..\Release\libexpat.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base
Source: "..\..\..\Release\libexpat.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base and pdb
Source: "..\..\..\Release\libtre.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base
Source: "..\..\..\Release\libtre.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base and pdb
Source: "..\..\..\Release\libnetxms.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base
Source: "..\..\..\Release\libnetxms.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base and pdb
Source: "..\..\..\Release\libnetxmsw.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base
Source: "..\..\..\Release\libnetxmsw.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base and pdb
; Console files
Source: "..\..\..\Release\nxzlib.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\nxzlib.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\libnxclw.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\libnxclw.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\libnxmapw.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\libnxmapw.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\libnxsnmpw.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\libnxsnmpw.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\nxuilib.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\nxuilib.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\scilexer.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\scilexer.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\nxlexer.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\nxlexer.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\nxcon.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\nxcon.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\nxav.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\nxav.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
Source: "..\..\..\Release\nxnotify.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console
Source: "..\..\..\Release\nxnotify.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: console and pdb
; Command-line tools files
Source: "..\..\..\Release\libnxcl.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\libnxcl.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools and pdb
Source: "..\..\..\Release\libnxmap.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\libnxmap.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools and pdb
Source: "..\..\..\Release\nxalarm.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\nxsms.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\nxevent.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\nxpush.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\libnxsl.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
Source: "..\..\..\Release\libnxsl.pdb"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools and pdb
Source: "..\..\..\Release\nxscript.exe"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: tools
; Third party files
Source: "..\..\install\windows\Files\Microsoft.VC80.CRT\*"; DestDir: "{app}\bin\Microsoft.VC80.CRT"; Flags: ignoreversion; Components: console
Source: "..\..\install\windows\Files\Microsoft.VC80.MFC\*"; DestDir: "{app}\bin\Microsoft.VC80.MFC"; Flags: ignoreversion; Components: console
Source: "..\..\install\windows\Files\libeay32.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base
;Source: "..\..\install\windows\Files\dbghelp.dll"; DestDir: "{app}\bin"; Flags: ignoreversion; Components: base

[Icons]
Name: "{group}\Alarm Notifier"; Filename: "{app}\bin\nxnotify.exe"; Components: console
Name: "{group}\Alarm Viewer"; Filename: "{app}\bin\nxav.exe"; Components: console
Name: "{group}\NetXMS Console"; Filename: "{app}\bin\nxcon.exe"; Components: console
Name: "{group}\{cm:UninstallProgram,NetXMS Management Console}"; Filename: "{uninstallexe}"
Name: "{userdesktop}\NetXMS Console"; Filename: "{app}\bin\nxcon.exe"; Tasks: desktopicon; Components: console
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\NetXMS Console"; Filename: "{app}\bin\nxcon.exe"; Tasks: quicklaunchicon; Components: console

[Code]
Var
  flagStartConsole: Boolean;

Function InitializeSetup(): Boolean;
Var
  i, nCount : Integer;
  param : String;
Begin
  // Set default values for flags
  flagStartConsole := FALSE;

  // Parse command line parameters
  nCount := ParamCount;
  For i := 1 To nCount Do Begin
    param := ParamStr(i);

    If Pos('/RUNCONSOLE', param) = 1 Then Begin
      flagStartConsole := TRUE;
    End;
  End;
  
  Result := TRUE;
End;

Procedure DeinitializeSetup;
Var
  strExecName: String;
  iResult: Integer;
Begin
  If flagStartConsole Then Begin
    strExecName := ExpandConstant('{app}\bin\nxcon.exe');
    If FileExists(strExecName) Then
    Begin
      Exec(strExecName, '', ExpandConstant('{app}\bin'), SW_SHOW, ewNoWait, iResult);
    End;
  End;
End;

