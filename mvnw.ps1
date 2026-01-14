# PowerShell Maven Wrapper
param(
    [Parameter(ValueFromRemainingArguments=$true)]
    $MavenArgs
)

$mavenHome = $env:MAVEN_HOME
if (-not $mavenHome) {
    $mavenHome = "C:\Users\basti\.maven\maven-3.9.12"
}

$mvnPath = Join-Path $mavenHome "bin\mvn.cmd"

if (-not (Test-Path $mvnPath)) {
    Write-Error "Maven not found at $mvnPath"
    exit 1
}

& $mvnPath @MavenArgs
