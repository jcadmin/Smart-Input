@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
echo Building Smart Input Pro Plugin...
gradlew.bat clean
gradlew.bat buildPlugin
echo Build completed! Plugin file is in build\distributions\
