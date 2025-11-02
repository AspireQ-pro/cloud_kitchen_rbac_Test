@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-22
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME set to: %JAVA_HOME%
mvn spring-boot:run