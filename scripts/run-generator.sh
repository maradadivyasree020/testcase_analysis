#!/usr/bin/env bash
# Run StoryReportGenerator using Maven exec with project classpath
set -euo pipefail

STORY_FILE=${1:-stories/ATT-123.yaml}
OUTPUT_FILE=${2:-reports/ATT-123-ai-report.md}

mkdir -p $(dirname "$OUTPUT_FILE")

mvn -Dexec.mainClass=com.example.tools.StoryReportGenerator \
    -Dexec.args="--storyFile=${STORY_FILE} --outputFile=${OUTPUT_FILE}" \
    -Dexec.cleanupDaemonThreads=false \
    org.codehaus.mojo:exec-maven-plugin:3.1.0:java

echo "Report written to $OUTPUT_FILE"
