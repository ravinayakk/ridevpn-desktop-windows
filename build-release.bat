@echo off
echo Building RideVPN Desktop Release Package

call mvn clean package

launch4j launch4j-config.xml

"C:\Program Files (x86)\Inno Setup 6\ISCC.exe" RideVPN-Installer.iss

echo Done, Created: output\RideVPN-Setup.exe
pause