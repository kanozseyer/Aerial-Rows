# AI Handoff Export History

This records how the repository handoff was created so a future model can understand both the archive and the reliability fixes applied to it.

## User requirement

The user requested an export of the project conversation inside Aerial-Rows that contains enough detail for a fresh ChatGPT conversation or another AI model to continue the project as effectively as the current conversation. The user explicitly asked to preserve every little detail, put the necessary files in GitHub, inspect failures, improve the process, and divide work into small resumable groups.

## Initial handoff commit

The first export attempt did not fail completely. It produced:

`b34d295407845ff1cb08e17d93875e888d3bbe24` — `Add complete AI project handoff and research context`

That commit added the orientation layer:

- `AGENTS.md`
- `AI_PROJECT_CONTEXT.md`
- `docs/ai-handoff/README.md`
- `C12_FINAL_BLUEPRINT.md`
- `CHAT_EXPORT.md`
- `COUCHY_DETAILED_FINDINGS.md`
- `PROJECTIVY_DETAILED_FINDINGS.md`
- `PROCESS_STATE.md`
- `RESEARCH_INDEX.md`

It did not yet include every raw research report.

## Resumable raw export

The raw export was then divided into groups E0–E9. Early raw Couchy reports used the GitHub Contents API with small files and frequent checkpoints.

Completed before the v3 retrofit:

- E2a: C1–C2
- E2b: C3
- E2c: C4
- E3a: C5
- E3b: C6

## Problems discovered

1. Large one-shot payloads could truncate or look stuck.
2. Many tiny Contents API commits were reliable but inefficient.
3. Chunk-boundary newlines were easy to mishandle manually.
4. C4 part 2 needed a one-byte repair during initial export.
5. C5 needed its chunk-boundary newline redistributed while preserving the full report SHA.
6. During the v3 retrofit audit, C3 part 3 was found to be one byte short on GitHub (`3844` bytes) versus the exact source chunk (`3845` bytes). This proved that prose claims of successful reassembly were not sufficient on their own.

## Process v3

The process was upgraded to raw Git objects + manifests + automated validation:

- exact report parts generated locally;
- exact Git blob SHA values precomputed;
- one manifest per report;
- one master raw archive manifest;
- reusable chunk and validation tools;
- GitHub Action validation;
- one atomic tree/commit per batch;
- branch ref updated with an `expected_sha` compare-and-swap lease.

The v3 retrofit repairs the C3 byte defect and standardizes C1–C6 without changing application source.

## Continuation

After the retrofit, raw Couchy C7 is the next pending report (E3c), followed by C8, C9–C12, Projectivy raw reports, process history, and final archive coverage validation.
