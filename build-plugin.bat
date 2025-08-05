@echo off
set JAVA_HOME=C:\Users\Administrator\.jdks\ms-17.0.15
set PATH=%JAVA_HOME%\bin;%PATH%
echo Building Smart Input Pro Plugin...
gradlew.bat buildPlugin
echo Build completed! Plugin file is in build\distributions\
