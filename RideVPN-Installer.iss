[Setup]
AppName=RideVPN
AppVersion=1.0.0
DefaultDirName={autopf}\RideVPN
DefaultGroupName=RideVPN
OutputDir=output
OutputBaseFilename=RideVPN-Setup
Compression=lzma2/max
SolidCompression=yes
PrivilegesRequired=admin

[Files]
Source: "build-dist\RideVPN.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "src\main\resources\bin\*"; DestDir: "{app}\bin"; Flags: ignoreversion recursesubdirs
Source: "build-dist\jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs

[Icons]
Name: "{group}\RideVPN"; Filename: "{app}\RideVPN.exe"
Name: "{autodesktop}\RideVPN"; Filename: "{app}\RideVPN.exe"

[Run]
Filename: "{app}\RideVPN.exe"; Description: "Launch RideVPN"; Flags: nowait postinstall skipifsilent shellexec

[UninstallRun]
Filename: "{app}\bin\wireguard.exe"; Parameters: "/uninstalltunnelservice ridevpn"; Flags: runhidden; RunOnceId: "RemoveTunnel"

[UninstallDelete]
Type: filesandordirs; Name: "{commonappdata}\RideVPN"
