; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

[Setup]
#include "setup.iss"
OutputBaseFilename=nxagent-1.2.2

[Files]
Source: "..\..\..\Release\libnetxms.dll"; DestDir: "{app}\bin"; BeforeInstall: StopService; Flags: ignoreversion
Source: "..\..\..\Release\libnxlp.dll"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\libnxdb.dll"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\nxagentd.exe"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\winnt.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\winperf.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\wmi.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\ping.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\portcheck.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\ecs.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\logwatch.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\odbcquery.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\sms.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\ups.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\informix.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\Release\oracle.nsm"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\contrib\nxagentd.conf-dist"; DestDir: "{app}\etc"; Flags: ignoreversion
Source: "..\..\..\release\libexpat.dll"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\..\release\libtre.dll"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\install\windows\files\libeay32.dll"; DestDir: "{app}\bin"; Flags: ignoreversion
Source: "..\..\install\windows\files\Microsoft.VC80.CRT\*"; DestDir: "{app}\bin\Microsoft.VC80.CRT"; Flags: ignoreversion

#include "common.iss"

Function GetCustomCmdLine(Param: String): String;
Begin
  Result := '';
End;

