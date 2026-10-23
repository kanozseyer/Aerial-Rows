# AI Handoff Export Process v5

This is the canonical procedure for the Launcher/Aerial-Rows handoff export.

## Guarantees

- The original research-report bytes and SHA-256 remain authoritative.
- Expected corpus coverage comes from `raw/SOURCE_CATALOG.json`, never memory or prose.
- Every completed report has a machine-readable manifest.
- The archive validator checks coverage, per-part hashes, Git blob hashes and exact reconstruction.
- Raw report publication is atomic: Git blobs -> one tree -> one commit -> compare-and-swap branch ref.
- Export work must not modify launcher implementation source.
- Proprietary APK/decompiled source/JADX/REA binaries are never committed.

## Storage formats

The validator is backward-compatible with the C1-C7 raw UTF-8 reports.

For C8 onward the preferred transport is `gzip-base64`:

1. verify the original `.md` against its authoritative catalog SHA;
2. gzip deterministically with `mtime=0`;
3. base64-encode the gzip bytes;
4. split the ASCII payload into bounded parts (default 4000 bytes for this connector);
5. publish those ASCII files as UTF-8 Git blobs;
6. reconstruct by concatenating -> base64 decode -> gzip decompress;
7. require the result to equal the original Markdown bytes exactly.

This is lossless. Compression is only a transport/storage representation and never changes the source identity.

## Packaging

```bash
python docs/ai-handoff/tools/package_report.py /path/to/source.md \
  --collection couchy --report-id C08 --storage gzip-base64 --max-bytes 4000
```

`package_report.py` refuses to write a report if the source SHA does not match `SOURCE_CATALOG.json`. It proves local exact reconstruction and updates the local archive manifest.

## Reconstructing a report

```bash
python docs/ai-handoff/tools/materialize_report.py \
  docs/ai-handoff/raw/couchy/C08.manifest.json \
  --output /tmp/couchy_group08.md
```

The materializer verifies source size and SHA before writing.

## Repository validation

Progress:

```bash
python docs/ai-handoff/tools/validate_archive.py
```

Final E9 closure:

```bash
python docs/ai-handoff/tools/validate_archive.py --require-complete
```

The full validator is intentionally a repository-checkout validator. Do not weaken it just because a temporary packaging directory lacks previously committed reports/context files.

## Atomic GitHub publication

For each subgroup:

1. fetch live `aerialrows-development` head/tree;
2. package and verify locally;
3. create each transport part as an exact Git blob;
4. require the returned blob SHA to equal the manifest-predicted Git blob SHA;
5. create one tree based on the live tree containing all report parts, manifest and state updates;
6. create one commit with the live head as parent;
7. move the branch using `expected_sha` compare-and-swap; never force over unknown concurrent work;
8. run/fetch the GitHub handoff validator;
9. compare against the research baseline to confirm documentation/workflow-only changes.

Unattached wrong/partial blobs are harmless and should simply be ignored.

## Connector-specific transport rule

`.md.gz.b64` parts are already base64 **text**. Create them with Git blob `encoding=utf-8`, not GitHub `encoding=base64`. Using `encoding=base64` would decode the text and store different bytes.

Keep transport chunks around 4000 ASCII bytes for this connector. Larger manual payloads have proven more error-prone.

## Failure/resume rule

If interrupted:

1. inspect branch head;
2. read `HANDOFF_MANIFEST.json`, `EXPORT_STATUS.md`, `SOURCE_CATALOG.json`, `ARCHIVE_MANIFEST.json`;
3. run the validator in a real checkout;
4. continue the first required catalog entry still `pending`;
5. never redo a validated complete report unless validation fails;
6. never trust prose over machine state.
