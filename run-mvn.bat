@echo off
REM Set the correct JAVA_HOME for this project
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot

REM Display the JAVA_HOME being used
echo Using JAVA_HOME: %JAVA_HOME%

REM Run Maven with the correct Java version
mvn %*