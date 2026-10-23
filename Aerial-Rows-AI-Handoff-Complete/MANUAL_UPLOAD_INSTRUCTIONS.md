# Manual Upload Instructions

The simplest durable repository setup is:

## Recommended

Upload `AI_HANDOFF_COMPLETE.md` to the repository root (or `docs/AI_HANDOFF_COMPLETE.md`). This is a single searchable Markdown file containing the project context, final blueprint, chat reconstruction, all Couchy C1–C12 reports, all Projectivy G1–G12 reports/post-closure audits, process state, and export continuation.

Also upload `Aerial-Rows-AI-Handoff-Complete-2026-10-23.zip` somewhere under `docs/ai-handoff/` as the lossless backup bundle if desired.

## Future AI prompt

Tell the new AI:

> Read `AI_HANDOFF_COMPLETE.md` first. Then verify the current `aerialrows-development` branch. The research programme is complete; continue from Phase 0A unless the handoff says otherwise.

## Why both files

- Markdown is directly searchable/readable by GitHub-connected AI tools.
- ZIP preserves the full file hierarchy and exact individual reports as a compact backup.
