const fs = require('fs');
const init = JSON.parse(fs.readFileSync('init_plan_phase.json', 'utf8'));
const agentSkills = fs.readFileSync('agent_skills_planner.txt', 'utf8').replace('AGENT_SKILLS_PLANNER=', '').trim();
const plannerContributions = fs.readFileSync('planner_contributions.txt', 'utf8');
const mvpMode = "false";
const walkingSkeleton = "false";

let prompt = `<planning_context>
**Phase:** ${init.phase_number}
**Mode:** standard

<files_to_read>
- ${init.state_path} (Project State)
- ${init.roadmap_path} (Roadmap)
- ${init.requirements_path} (Requirements)
- ${init.context_path} (USER DECISIONS from /gsd-discuss-phase)
- .planning/phases/01-security-database-stabilization/01-RESEARCH.md (Technical Research)
</files_to_read>

${agentSkills}

**Phase requirement IDs (every ID MUST appear in a plan's \`requirements\` field):** ${init.phase_req_ids}

**Project instructions:** Read ./AGENTS.md or ./.opencode/AGENTS.md if either exists — follow project-specific guidelines
**Project skills:** Check .claude/skills/ or .agents/skills/ directory (if either exists) — read SKILL.md files, plans should account for project skill rules

${plannerContributions}

**MVP_MODE:** ${mvpMode} (when true, follow vertical-slice rules from \`/data/data/com.termux/files/home/.config/opencode/gsd-core/references/planner-mvp-mode.md\`; when false, ignore MVP guidance entirely.)
**WALKING_SKELETON:** ${walkingSkeleton} (when true, the first deliverable must be a Walking Skeleton — Read the template at \`/data/data/com.termux/files/home/.config/opencode/gsd-core/references/skeleton-template.md\` and produce SKELETON.md alongside PLAN.md.)
**Granularity:** ${init.granularity}

</planning_context>

<downstream_consumer>
Output consumed by /gsd-execute-phase. Plans need:
- Frontmatter (wave, depends_on, files_modified, autonomous)
- Tasks in XML format with read_first and acceptance_criteria fields (MANDATORY on every task)
- Verification criteria
- must_haves for goal-backward verification
- **"Artifacts this phase produces" section (MANDATORY)** — list every symbol this phase creates: decorators, classes, functions, CLI flags, struct/dataclass fields, new file paths. The plan-review-convergence source-grounding pass reads this section to exclude newly-created symbols from drift verification; omitting it causes new symbols to be flagged for acknowledgement.
</downstream_consumer>

<deep_work_rules>
## Anti-Shallow Execution Rules (MANDATORY)

Every task MUST include these fields — they are NOT optional:

1. **\`<read_first>\`** — Files the executor MUST read before touching anything. Always include:
   - The file being modified (so executor sees current state, not assumptions)
   - Any "source of truth" file referenced in CONTEXT.md (reference implementations, existing patterns, config files, schemas)
   - Any file whose patterns, signatures, types, or conventions must be replicated or respected

2. **\`<acceptance_criteria>\`** — Verifiable conditions that prove the task was done correctly. Rules:
   - Every criterion must be checkable as a source assertion, behavior assertion, test command, or CLI output
   - NEVER use subjective language ("looks correct", "properly configured", "consistent with")
   - Include exact strings, patterns, values, command outputs, or observable behavior where that is the right proof
   - Examples:
     - Code: \`auth.py contains def verify_token(\` / \`test_auth.py exits 0\`
     - Behavior: \`POST /api/auth/login returns 200 + httpOnly JWT cookie for valid credentials\`
     - Config: \`.env.example contains DATABASE_URL=\` / \`Dockerfile contains HEALTHCHECK\`
     - Docs: \`README.md contains '## Installation'\` / \`API.md lists all endpoints\`
     - Infra: \`deploy.yml has rollback step\` / \`docker-compose.yml has healthcheck for db\`

3. **\`<action>\`** — Must include CONCRETE values, not references. Rules:
   - NEVER say "align X with Y", "match X to Y", "update to be consistent" without specifying the exact target state
   - Include concrete identifiers and reference values: config keys, function signatures, SQL table names, class names, import paths, env vars, endpoint paths, etc.
   - If CONTEXT.md has a comparison table or expected values, copy only the target identifiers/values needed to remove ambiguity
   - Do not include full file contents, fenced code blocks, or complete implementations in \`<action>\`
   - The executor should understand the intended target state from \`<action>\` and use \`<read_first>\` files for current implementation details, patterns, and source-of-truth context

**Why this matters:** Executor agents work from the plan text. Vague instructions like "update the config to match production" produce shallow one-line changes. Concrete instructions like "add DATABASE_URL, set POOL_SIZE=20, add REDIS_URL, and read config/runtime.ts before editing" produce complete work without turning the planner into the executor.
</deep_work_rules>

<quality_gate>
- [ ] PLAN.md files created in phase directory
- [ ] Each plan has valid frontmatter
- [ ] Tasks are specific and actionable
- [ ] Every task has \`<read_first>\` with at least the file being modified
- [ ] Every task has \`<acceptance_criteria>\` with behavior, test-command, CLI, or source assertions
- [ ] Every \`<action>\` contains concrete identifiers without fenced code blocks or full implementations
- [ ] Dependencies correctly identified
- [ ] Waves assigned for parallel execution
- [ ] must_haves derived from phase goal
- [ ] Every PLAN.md includes an "Artifacts this phase produces" section listing symbols created by this phase (decorators, classes, functions, CLI flags, struct/dataclass fields, new file paths)
- [ ] Every SPEC ## Edge Coverage covered/backstop edge is represented in a plan's must_haves (no silent drops)
- [ ] Every UI-SPEC ## UI Considerations covered/backstop consideration is represented in a plan's must_haves (no silent drops)
- [ ] Every SPEC ## Prohibitions resolved item is represented in a plan's must_haves.prohibitions (no silent drops)
</quality_gate>
`;

fs.writeFileSync('filled_planner_prompt.txt', prompt);
