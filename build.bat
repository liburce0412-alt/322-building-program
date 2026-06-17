@echo off
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "ANDROID_HOME=C:\Users\28219\AppData\Local\Android\Sdk"
cd /d C:\Users\28219\Desktop\program
call gradlew.bat assembleDebug --no-daemon
