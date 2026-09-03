@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "XIAOLIAO_UPLOAD_BASE_URL=https://xiaoliao.natapp1.cc"
echo Starting xiaoliao-api on 8080 ...
"C:\Users\Administrator\.jdks\ms-17.0.20\bin\java.exe" -jar "xiaoliao-api\target\xiaoliao-api-0.1.0-SNAPSHOT.jar"
pause