# Chat Export Appendix — Handoff Export Work

After C12 the user requested that the full project/chat context be stored inside Aerial-Rows with enough detail for a new ChatGPT conversation or another AI model to continue equivalently. The user required every little detail, asked that failed/stuck work be inspected and repaired, asked for small resumable groups, and repeatedly requested process improvements before continuing.

The initial attempt had produced the orientation/context commit `b34d295407845ff1cb08e17d93875e888d3bbe24` but not the complete raw corpus. Early Contents-API chunking made C1–C6 durable but was slow and exposed newline-boundary errors. A retroactive audit found C3 part 3 one byte short despite an earlier prose checkpoint claiming exact reconstruction.

Export Process v3 replaced that with exact Git blobs, per-report manifests, master archive state, automated repository validation, atomic report commits and compare-and-swap branch-ref updates. It repaired C3 and standardized C1–C6.

The user asked for another reliability/efficiency/speed audit. Export Process v4 adds `SOURCE_CATALOG.json` containing the authoritative expected SHA-256 for every Couchy C1–C12 and every required Projectivy original/post-closure report. The validator checks both bytes and corpus coverage; unknown/missing/wrong-SHA reports are errors. `package_report.py` verifies source identity before producing files. C07 was the first full v4-packaged report.

The user then asked for another process audit before continuing. C8 transport testing showed that the remaining weakness was manual raw/base64 payload size rather than validation or atomic publication. Export Process v5 therefore uses deterministic `gzip+base64` storage for new reports. This is lossless: the original Markdown SHA remains authoritative, and the validator/materializer decode and decompress the stored chunks and require exact original bytes. The v5 validator remains backward-compatible with C1-C7 raw UTF-8 reports. C08 is the first report stored in this compressed representation.

An important transport correction was discovered during C8: `.md.gz.b64` chunks are themselves ASCII base64 text, so GitHub blob creation must use `encoding=utf-8`. Passing them to GitHub with `encoding=base64` would store the decoded gzip bytes and produce a different blob identity. That rule is now documented and enforced by predicted Git blob SHA checks.

See `EXPORT_PROCESS.md`, `EXPORT_HISTORY.md`, `EXPORT_STATUS.md`, `raw/SOURCE_CATALOG.json` and `raw/ARCHIVE_MANIFEST.json` for canonical state.
