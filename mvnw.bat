@echo off
rem Maven wrapper batch script for Windows

setlocal enabledelayedexpansion

if not exist pom.xml (
    echo Error: pom.xml not found. Run this from the project root directory.
    exit /b 1
)

rem Check if Maven is in PATH
where mvn >nul 2>nul
if %errorlevel% equ 0 (
    mvn %*
) else if defined MAVEN_HOME (
    "%MAVEN_HOME%\bin\mvn" %*
) else (
    echo Maven not found in PATH. 
    echo Please install Maven from: https://maven.apache.org/download.cgi
    echo Or set MAVEN_HOME environment variable pointing to Maven installation directory.
    exit /b 1
)
