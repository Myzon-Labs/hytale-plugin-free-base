#!/usr/bin/env pwsh
# Maven wrapper script for Windows (PowerShell)

$MAVEN_HOME = $env:MAVEN_HOME
if (-not $MAVEN_HOME) {
    $MAVEN_HOME = "C:\Users\basti\.maven\maven-3.9.12"
}

$mvnCmd = Join-Path $MAVEN_HOME "bin\mvn.cmd"

if (-not (Test-Path $mvnCmd)) {
    Write-Host "Maven not found at $mvnCmd"
    Write-Host "Please install Maven or set MAVEN_HOME environment variable."
    Write-Host "Download from: https://maven.apache.org/download.cgi"
    exit 1
}

& $mvnCmd $args
