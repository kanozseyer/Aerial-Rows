# AI Handoff Export History

The user requested a project/chat export inside Aerial-Rows detailed enough for a fresh ChatGPT conversation or another AI model to continue equivalently, and repeatedly asked that failed/stuck work be inspected, made smaller/resumable, and improved for reliability, efficiency and speed.

## Initial orientation handoff

Commit `b34d295407845ff1cb08e17d93875e888d3bbe24` added the orientation layer but not every raw report.

## Early raw export problems

Large one-shot work could look stuck; many tiny Contents-API commits were slow; newline chunk boundaries were easy to mishandle; C4/C5 needed boundary repair; and a later audit found C3 part 3 one byte short despite a prior prose claim of exact reconstruction.

## v3

v3 moved to exact Git blobs, per-report manifests, master archive manifest, automated validation, atomic trees/commits and compare-and-swap branch updates. It repaired C3 and standardized C1–C6. Checkpoint `f4997de730d869bc951c7ba25d8e61249ad7c38d` validated successfully in GitHub Actions.

## v4

The remaining weakness was coverage definition. v4 adds `raw/SOURCE_CATALOG.json` with authoritative SHA-256 values for every Couchy C1–C12 and required Projectivy original/post-closure report; machine-catalogs the final audit contract/process-bundle reference hashes; makes validation reject missing/unknown IDs and wrong source hashes; adds `--require-complete` for final closure; and adds `package_report.py`, which verifies a source against the catalog before writing archive output and updates local archive state automatically.

The v4 bootstrap itself used a few small documentation commits while the new tooling was being installed; raw report publication remains the place where atomic Git-object publication is mandatory. C07 is the first report to use the complete catalog-aware v4 workflow.
