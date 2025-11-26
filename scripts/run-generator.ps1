param(
    [string]$StoryFile = "stories/ATT-123.yaml",
    [string]$OutputFile = "reports/ATT-123-ai-report.md"
)

if (-not (Test-Path (Split-Path $OutputFile))) {
    New-Item -ItemType Directory -Path (Split-Path $OutputFile) -Force | Out-Null
}

mvn -Dexec.mainClass=com.example.tools.StoryReportGenerator `
    -Dexec.args="--storyFile=$StoryFile --outputFile=$OutputFile" `
    -Dexec.cleanupDaemonThreads=false `
    org.codehaus.mojo:exec-maven-plugin:3.1.0:java

Write-Host "Report written to $OutputFile"
