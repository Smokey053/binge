param(
    [ValidateSet('compile', 'run')]
    [string]$Goal = 'run'
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($env:TMDB_API_KEY)) {
    $env:TMDB_API_KEY = [Environment]::GetEnvironmentVariable('TMDB_API_KEY', 'User')
}
if ([string]::IsNullOrWhiteSpace($env:TMDB_API_KEY)) {
    Write-Warning 'TMDB_API_KEY is not configured. Binge will use fallback artwork.'
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($maven) {
    $mavenPath = $maven.Source
} else {
    $mavenPath = Get-ChildItem "$HOME\.vscode\extensions\oracle.oracle-java-*\nbcode\java\maven\bin\mvn.cmd" `
        -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mavenPath) {
    throw 'Maven was not found. Install Maven or add it to PATH.'
}

$mavenGoal = if ($Goal -eq 'run') { 'javafx:run' } else { 'compile' }
& $mavenPath `
    -f "$PSScriptRoot\..\binge\pom.xml" `
    $mavenGoal
