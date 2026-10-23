## v5 — lossless compressed transport

C8 exposed that raw/base64 payload transfer through the chat connector remained the slowest and most error-prone step. Markdown compresses well, so v5 stores new reports as deterministic gzip bytes encoded as ASCII base64 chunks while retaining the original Markdown SHA as the authoritative identity.

The validator is backward-compatible with C1-C7 raw UTF-8 manifests. New `materialize_report.py` reconstructs an exact `.md` from either representation. A critical connector rule was recorded: `.gz.b64` chunks are ASCII base64 text and must be sent to GitHub blob creation using UTF-8 encoding, not GitHub's base64 transport mode.
