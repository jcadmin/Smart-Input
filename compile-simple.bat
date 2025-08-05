@echo off
echo Compiling with Java 17...
set PATH=C:\Users\Administrator\.jdks\ms-17.0.16\bin;%PATH%
set JAVA_HOME=

echo Java version:
java -version

echo Compiling Kotlin...
gradlew.bat compileKotlin
