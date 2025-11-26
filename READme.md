CI and generator helper

How to run the StoryReportGenerator locally

Bash:

```bash
# set required env vars
export LLM_BASE_URL="https://your-llm.example"
export LLM_API_KEY="sk-..."

# run using the included script
./scripts/run-generator.sh stories/ATT-123.yaml reports/ATT-123-ai-report.md
```

PowerShell:

```powershell
$env:LLM_BASE_URL = "https://your-llm.example"
$env:LLM_API_KEY = "sk-..."
.
\scripts\run-generator.ps1 -StoryFile stories/ATT-123.yaml -OutputFile reports/ATT-123-ai-report.md
```

CI notes

- The GitHub Actions workflow `/.github/workflows/ci.yml` runs the generator only when `LLM_BASE_URL` and `LLM_API_KEY` are configured as repo secrets. When secrets are missing a placeholder report is written to `reports/ATT-123-ai-report.md` so artifact upload doesn't fail.
- The workflow runs tests and uploads JUnit reports only if `target/surefire-reports` exists.
