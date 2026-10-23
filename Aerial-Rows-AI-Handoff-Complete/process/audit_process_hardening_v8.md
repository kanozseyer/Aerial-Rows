# Audit Process Hardening v8 — Connection-Interruption / Last-Mile Recovery

## Failure diagnosis

The interrupted Group 10 run did **not** lose or corrupt reverse-engineering work. Durable state proves that `AUDIT_10_R2` had already completed discovery, verification, synthesis and report before the visible conversation stopped:

- `resume_from = none`;
- 23/23 canonical REA evidence files validate successfully;
- all 11 closure dimensions are covered or explicitly not applicable;
- the final Group 10 v2 report exists with a matching SHA-256 sidecar;
- the evidence snapshot passes;
- no stale audit temp files or orphaned REA/JADX analysis workers remain.

The exact browser/platform transport cause is not observable from the analysis container. The durable timeline therefore supports a **last-mile chat/response interruption after analytical completion**, not an evidence or checkpoint failure.

## Secondary orchestration weakness found

The old last-mile path kept doing expensive work after the report was already durable:

1. snapshot verification;
2. preflight (which verified the same snapshot again);
3. a full global self-test;
4. verification of every historical completed audit;
5. serialization of the full APK resource inventory in diagnostic output.

That could add 15+ seconds of duplicated work after the result was already ready, which is exactly the wrong place to risk a chat interruption.

A separate diagnostic command also combined several historical re-attestations and a full self-test into one outer shell invocation. Individual operations were healthy, but the combined command exceeded the outer execution window.

**Rule:** critical handoff operations are now individually bounded and are never chained behind historical maintenance or verbose diagnostics.

## Process contract v18

The process now has an explicit durable completion boundary plus an O(1)-style recovery path.

New/strengthened policies include:

- completion receipt is the canonical user-handoff record;
- finalisation runs immediately after report completion;
- current audit snapshot is verified once, inside preflight;
- compact **core** self-test is used during handoff;
- full/deep self-test is separate maintenance and may verify all completed audits;
- self-test still writes full diagnostics to disk but can print a compact summary;
- batch state is written **before** entering an expensive REA/JADX call;
- an interrupted batch therefore records the exact `in_progress` item, attempt count and recovery count;
- a fast `ready` command validates an existing completion receipt and skips expensive rechecks when nothing changed;
- receipt validity is tied to report/state/manifest hashes, process contract SHA/version, process-tool bundle SHA and target APK SHA.

## Write-ahead batch recovery

Before every expensive probe the batch runner atomically records:

- item id;
- `status = in_progress`;
- output path;
- start time;
- attempt count;
- recovered-interruption count.

A regression test simulated a killed probe by leaving an item `in_progress`. The next run recovered it correctly:

    attempts                         2
    recovered_interrupted_attempts  1
    final status                     complete

The retry completed from the persistent JADX cache.

## Compact finalisation

Canonical command:

    python3 auditctl.py finalize AUDIT_NAME --report /path/to/report.md --process-doc /path/to/process.md

It refuses to create a handoff receipt unless:

1. report stage is complete and `resume_from=none`;
2. report SHA-256 sidecar matches the report bytes;
3. current audit preflight passes;
4. the completed evidence snapshot passes inside preflight;
5. compact core runtime/toolchain self-test passes;
6. optional process-document SHA sidecar matches.

It atomically writes:

- `audits/<AUDIT>/completion_receipt.json`;
- `audit_system/last_completion_receipt.json`.

Measured Group 10 v18 finalisation: approximately **8.07 seconds** total.

## Fast interruption recovery

First command after any suspected interruption:

    python3 auditctl.py ready AUDIT_NAME

It validates:

- completion receipt exists and is marked ready;
- report hash;
- state hash;
- evidence-manifest hash;
- current process contract version and SHA;
- current process-tool bundle SHA;
- target APK SHA.

If all match, the result is safe to hand to the user immediately without repeating preflight or self-tests.

Measured Group 10 recovery: approximately **0.79 seconds**.

If the receipt is stale or absent, normal deterministic recovery applies: inspect `state.json` / `resume_from` and continue only the unfinished stage/item.

## Deep maintenance remains available

A full self-test still verifies all completed audit snapshots and remains mandatory after process/tool changes before those snapshots are re-attested.

Current full/deep self-test time: approximately **10.49 seconds** for four completed audits.

Current core handoff self-test time: approximately **3.8 seconds**.

## Current stable health

    Process contract          v18
    Full/deep self-test       PASS
    SQLite integrity          PASS
    Persistent JADX           PASS
    Class inventory           7,395 / 7,395
    APK/resource provenance   PASS
    Stale audit temps         0
    Shared transients         0
    Completed snapshots       4 / 4 PASS

Current process contract SHA-256:

`052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`

Current process-tool bundle SHA-256:

`251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Group 10 state

`AUDIT_10_R2` remains complete and does not need to be rerun:

- 23/23 canonical evidence files successful;
- 0 canonical errors/invalid records;
- 11/11 closure dimensions closed;
- report snapshot verified;
- final report SHA-256: `d75640cc4a23d2a7d392ee3322a50d59c942fad9c09094b4e38a58c130d2c126`.
