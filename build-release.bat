@echo off
cd /d "%~dp0"

echo Building jar...
call mvn clean package || goto :fail

echo Building Java runtime...
rmdir /s /q build-dist\jre 2>nul
jlink --add-modules java.base,java.desktop,java.logging,java.net.http,java.scripting,java.xml,jdk.unsupported,jdk.crypto.ec --output build-dist\jre || goto :fail

echo Building RideVPN.exe...
launch4jc launch4j-config.xml || goto :fail

echo Building installer...
"C:\Program Files (x86)\Inno Setup 6\ISCC.exe" RideVPN-Installer.iss || goto :fail

echo Done: output\RideVPN-Setup.exe
pause
exit /b 0

:fail
echo BUILD FAILED
pause
exit /b 1
