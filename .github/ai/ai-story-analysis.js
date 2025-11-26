// .github/ai/ai-story-analysis.js
import fs from "fs";
import path from "path";
import { Octokit } from "@octokit/rest";
import OpenAI from "openai";
import pRetry from "p-retry";

const {
  OPENAI_API_KEY,
  GITHUB_TOKEN,
  GITHUB_REPOSITORY,
  GITHUB_EVENT_PATH,
  MAX_FILES = "8",
  MAX_PATCH_CHARS = "1500",
  SAVE_PATH = ".ai",
} = process.env;

if (!GITHUB_EVENT_PATH) throw new Error("GITHUB_EVENT_PATH not set");
if (!GITHUB_REPOSITORY) throw new Error("GITHUB_REPOSITORY not set");

const event = JSON.parse(fs.readFileSync(GITHUB_EVENT_PATH, "utf8"));
const prNumber = event?.pull_request?.number;
if (!prNumber) {
  console.log("No pull request number in event — exiting.");
  process.exit(0);
}

const [owner, repo] = GITHUB_REPOSITORY.split("/");
const octokit = new Octokit({ auth: GITHUB_TOKEN });
const openai = new OpenAI({ apiKey: OPENAI_API_KEY });

const truncate = (s, n) => (s && s.length > n ? s.slice(0, n) + "\n\n...[truncated]" : s);

async function listRelevantFiles() {
  const resp = await octokit.pulls.listFiles({ owner, repo, pull_number: prNumber, per_page: 200 });
  // Filter to Java-relevant files (pom.xml, src/)
  let files = resp.data.filter(f =>
    f.filename.startsWith("src/") || f.filename === "pom.xml" || f.filename.startsWith("src-test/") || f.filename.includes(".java")
  );
  files = files.slice(0, Number(MAX_FILES));
  return files.map(f => ({
    filename: f.filename,
    status: f.status,
    patch: f.patch ? truncate(f.patch, Number(MAX_PATCH_CHARS)) : "(binary or no patch)",
  }));
}

function buildPrompt(prTitle, prBody, changedFiles, diagramPath) {
  const header = `You are a senior QA/BA assistant. Given the PR title, description and code diffs (Java backend), provide:
1) 4-line summary
2) Acceptance Criteria (Given-When-Then)
3) 6-12 functional test cases (positive & negative)
4) File risks / quick review notes

Return markdown with sections: ## Summary, ## Acceptance Criteria, ## Test Cases, ## File Risks.
`;
  const filesText = changedFiles.map(f => `### ${f.filename}\nStatus: ${f.status}\n\`\`\`\n${f.patch}\n\`\`\``).join("\n\n");
  const diagramNote = diagramPath ? `\n\nReference diagram (uploaded): ${diagramPath}\n` : "";
  return `${header}\nPR Title: ${prTitle}\n\nPR Description:\n${prBody || "(no description)"}\n\nChanged files (truncated):\n${filesText}\n${diagramNote}\n\nNotes:\n- Keep acceptance criteria testable.\n- Mention required test data/setup.\n`;
}

async function callOpenAI(prompt) {
  return pRetry(
    async () => {
      const resp = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: [{ role: "user", content: prompt }],
        max_tokens: 1200,
        temperature: 0.0,
      });
      const text = resp?.choices?.[0]?.message?.content;
      if (!text) throw new Error("Empty response from OpenAI");
      return text;
    },
    { retries: 3, factor: 2, minTimeout: 1000 }
  );
}

async function saveAndComment(markdown) {
  const outDir = path.join(process.cwd(), SAVE_PATH);
  if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });
  const filePath = path.join(outDir, `analysis-pr-${prNumber}.md`);
  fs.writeFileSync(filePath, `<!-- AI Analysis -->\n\n${markdown}`, "utf8");
  const comment = `### 🤖 AI Story Analysis\n\n${markdown}\n\n---\n**Diagram reference:** \n\`${"/mnt/data/e196d04e-cfe4-41a7-bc24-4f019e6d73ac.png"}\``;
  await octokit.issues.createComment({ owner, repo, issue_number: prNumber, body: comment });
  console.log("Saved analysis and posted comment.");
}

(async () => {
  try {
    const pr = await octokit.pulls.get({ owner, repo, pull_number: prNumber });
    const changed = await listRelevantFiles();
    const prompt = buildPrompt(pr.data.title, pr.data.body || "", changed, "/mnt/data/e196d04e-cfe4-41a7-bc24-4f019e6d73ac.png");
    const analysis = await callOpenAI(prompt);
    await saveAndComment(analysis.trim());
  } catch (err) {
    console.error("AI analysis failed:", err);
    try {
      await octokit.issues.createComment({ owner, repo, issue_number: prNumber, body: `⚠️ AI analysis failed: ${err.message}` });
    } catch(e) { console.error("Failed to post error comment:", e); }
    process.exit(1);
  }
})();
