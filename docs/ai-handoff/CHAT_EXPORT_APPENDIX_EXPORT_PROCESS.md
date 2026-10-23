# Chat Export Appendix — Handoff Export Work

This appendix records the conversation that occurred after C12 when the user asked for the complete project/chat export.

The user requested that the chat/project context be put inside the Aerial-Rows GitHub sources with enough detail for a new ChatGPT conversation or another AI model to continue as effectively as the current one. The user required every little detail, asked that failed/stuck export work be inspected and repaired, and asked that the workload be divided into smaller resumable groups.

The initial export attempt appeared stuck because it tried to stage large archive blobs before final tree/commit publication. Inspection showed it had actually produced commit `b34d295407845ff1cb08e17d93875e888d3bbe24`, containing the orientation/context layer but not the complete raw corpus.

A resumable E0–E9 plan was introduced. Raw Couchy C1–C6 were exported in small checkpoints. C3/C4 initially used six ~180-line chunks; C5 used three chunks; C6 moved toward larger ~14 KiB chunks.

The user then explicitly asked to re-audit the export process for reliability, efficiency and speed and apply improvements retroactively to all export work completed so far.

That re-audit led to Export Process v3: exact Git blobs, per-report manifests, a master archive manifest, automated validation, atomic tree/commit batches and compare-and-swap branch ref updates. During the retrofit a real legacy error was detected: Couchy C3 part 3 was one byte short despite an earlier prose checkpoint claiming exact reassembly. The v3 retrofit repairs that byte and makes machine validation authoritative over prose status claims.

See `EXPORT_PROCESS.md`, `EXPORT_HISTORY.md` and `EXPORT_STATUS.md` for the canonical procedure and current continuation point.
