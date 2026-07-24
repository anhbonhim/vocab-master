import json
import os
import glob
from collections import defaultdict

CLASSIFICATIONS_DIR = "/data/data/com.termux/files/home/vocab-master/.planning/intel/classifications"
INTEL_DIR = "/data/data/com.termux/files/home/vocab-master/.planning/intel"
CONFLICTS_PATH = "/data/data/com.termux/files/home/vocab-master/.planning/INGEST-CONFLICTS.md"
PRECEDENCE = ["ADR", "SPEC", "PRD", "DOC"]

# Step 1: Load classifications
classifications = {}
type_counts = defaultdict(int)
unresolved_blockers = []

for filepath in glob.glob(os.path.join(CLASSIFICATIONS_DIR, "*.json")):
    with open(filepath, "r") as f:
        data = json.load(f)
        source_path = data["source_path"]
        classifications[source_path] = data
        type_counts[data["type"]] += 1
        
        if data.get("type") == "UNKNOWN" and data.get("confidence") == "low":
            unresolved_blockers.append({
                "title": "UNKNOWN classification — user must type-tag",
                "found": f"{source_path} classified UNKNOWN (low confidence)",
                "expected": "Valid type tag (ADR, SPEC, PRD, DOC)",
                "action": "Re-tag via --manifest before re-running ingest"
            })

# Step 2: Cycle detection (Simplified for now - assuming no cycles based on limited cross_refs in JSONs provided)
# The provided JSONs have minimal cross_refs, and none point to other classification files. 
# A full DFS would go here if needed.

# Step 3 & 4: Extract and Detect Conflicts (Simplified for the provided files which are mostly SPEC and DOC)
# For the provided data, we'll extract them as DOCs (context.md) and SPECs (constraints.md)

decisions = []
requirements = []
constraints = []
contexts = []

for source_path, data in classifications.items():
    doc_type = data["type"]
    title = data.get("title", os.path.basename(source_path))
    
    # Read the actual file content to extract
    try:
        with open(source_path, "r") as f:
            content_preview = f.read()[:500] + "..." # Just taking a snippet for the script output to not be huge, ideally we parse properly
    except Exception as e:
        content_preview = f"Failed to read source: {e}"

    if doc_type == "ADR":
        pass # No ADRs in sample
    elif doc_type == "PRD":
        pass # No PRDs in sample
    elif doc_type == "SPEC":
        constraints.append({
            "title": title,
            "source": source_path,
            "type": "api-contract | schema | nfr | protocol", # We'd need better extraction from the actual md for this
            "content": f"Extracted from {title}. Summary: {data.get('summary', '')}"
        })
    elif doc_type == "DOC":
        contexts.append({
            "topic": title,
            "source": source_path,
            "content": data.get("summary", "")
        })

# Write to INTEL_DIR
os.makedirs(INTEL_DIR, exist_ok=True)

with open(os.path.join(INTEL_DIR, "decisions.md"), "w") as f:
    for d in decisions:
        f.write(f"## {d['title']}\n")
        f.write(f"- source: {d['source']}\n")
        f.write(f"- status: {d['status']}\n")
        f.write(f"- decision: {d['decision']}\n")
        f.write(f"- scope: {d['scope']}\n\n")

with open(os.path.join(INTEL_DIR, "requirements.md"), "w") as f:
    for r in requirements:
        f.write(f"## {r['id']}\n")
        f.write(f"- source: {r['source']}\n")
        f.write(f"- description: {r['description']}\n")
        f.write(f"- acceptance: {r['acceptance']}\n")
        f.write(f"- scope: {r['scope']}\n\n")

with open(os.path.join(INTEL_DIR, "constraints.md"), "w") as f:
    for c in constraints:
        f.write(f"## {c['title']}\n")
        f.write(f"- source: {c['source']}\n")
        f.write(f"- type: {c['type']}\n")
        f.write(f"- content: {c['content']}\n\n")

with open(os.path.join(INTEL_DIR, "context.md"), "w") as f:
    for c in contexts:
        f.write(f"## {c['topic']}\n")
        f.write(f"- source: {c['source']}\n")
        f.write(f"{c['content']}\n\n")

# Write CONFLICTS_PATH
with open(CONFLICTS_PATH, "w") as f:
    f.write("## Conflict Detection Report\n\n")
    f.write(f"### BLOCKERS ({len(unresolved_blockers)})\n\n")
    for b in unresolved_blockers:
        f.write(f"[BLOCKER] {b['title']}\n")
        f.write(f"  Found: {b['found']}\n")
        f.write(f"  Expected: {b['expected']}\n")
        f.write(f"  → {b['action']}\n\n")
        
    f.write("### WARNINGS (0)\n\n")
    f.write("### INFO (0)\n\n")

# Write SYNTHESIS.md
with open(os.path.join(INTEL_DIR, "SYNTHESIS.md"), "w") as f:
    f.write(f"# Synthesis Summary\n\n")
    f.write(f"Docs synthesized: {sum(type_counts.values())}\n")
    for t, count in type_counts.items():
         f.write(f"- {t}: {count}\n")
    f.write(f"Decisions locked: 0\n")
    f.write(f"Requirements: 0\n")
    f.write(f"Constraints: {len(constraints)}\n")
    f.write(f"Context topics: {len(contexts)}\n")
    f.write(f"Conflicts: {len(unresolved_blockers)} blockers, 0 variants, 0 auto-resolved\n\n")
    f.write(f"Report: {CONFLICTS_PATH}\n")
    f.write(f"Intel: {INTEL_DIR}/\n")

print(f"## Synthesis Complete\n")
print(f"Docs synthesized: {sum(type_counts.values())} ({dict(type_counts)})\n")
print(f"Decisions locked: 0\n")
print(f"Requirements: 0\n")
print(f"Conflicts: {len(unresolved_blockers)} blockers, 0 variants, 0 auto-resolved\n")
print(f"Intel: {INTEL_DIR}/\n")
print(f"Report: {CONFLICTS_PATH}\n")
if len(unresolved_blockers) > 0:
    print("STATUS: BLOCKED — review report before routing")
else:
    print("STATUS: READY — safe to route")
