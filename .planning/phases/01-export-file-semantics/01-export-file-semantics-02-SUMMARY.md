---
phase: 01-export-file-semantics
plan: 02
subsystem: testing
tags: [java, gradle, junit, nio, filesystem, burp-export]
requires:
  - phase: 01-export-file-semantics
    provides: deterministic host-and-path filename derivation via ExportFileName and FilenamePolicy
provides:
  - collision-safe raw request file writing with CREATE_NEW semantics
  - prepared export value objects for target directory, derived filename, and request bytes
  - unit coverage for byte fidelity, suffix retries, directory creation, and input guardrails
affects: [phase-2-burp-save-workflow, phase-3-output-location-defaults]
tech-stack:
  added: []
  patterns: [immutable export payload value objects, Java NIO CREATE_NEW retry loop, byte-preserving raw request export]
key-files:
  created:
    - src/main/java/com/example/burprequestsaver/model/ExportTarget.java
    - src/main/java/com/example/burprequestsaver/model/PreparedExport.java
    - src/main/java/com/example/burprequestsaver/export/RawRequestWriter.java
    - src/test/java/com/example/burprequestsaver/export/RawRequestWriterTest.java
  modified: []
key-decisions:
  - "Treat prepared exports as immutable value objects so downstream Burp wiring passes a typed target, filename, and raw byte payload together."
  - "Use Files.write with CREATE_NEW and monotonic -1, -2, -3 suffix retries so repeated exports never overwrite earlier files."
  - "Validate null filename, null output directory, and empty request bytes at the writer boundary before any filesystem work."
patterns-established:
  - "PreparedExport bundles ExportTarget, ExportFileName, and raw request bytes for the write layer."
  - "RawRequestWriter resolves only sanitized final filenames against the chosen output directory and retries on FileAlreadyExistsException."
requirements-completed: [OUT-01, OUT-02, OUT-05, QUAL-03]
duration: 4 min
completed: 2026-04-09
---

# Phase 1 Plan 2: Export Write Core Summary

**Collision-safe raw request export writing with immutable payload contracts and exact byte round-trip coverage.**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-09T16:35:44Z
- **Completed:** 2026-04-09T16:38:42Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- Added `ExportTarget` and `PreparedExport` to carry the output directory, derived filename, and raw request bytes through the export core.
- Implemented `RawRequestWriter.write(PreparedExport)` with automatic directory creation, atomic `CREATE_NEW` writes, and monotonic collision suffix retries.
- Locked in byte fidelity, suffix retry behavior, nested directory creation, and writer guardrails with focused JUnit tests.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define the prepared export contracts and writer tests** - `038ab7f` (test)
2. **Task 2: Implement the raw request writer with atomic create semantics** - `d9d85ca` (feat)
3. **Task 3: Run the complete Phase 1 unit suite and harden the export core for downstream use** - `ba54673` (test)

## Files Created/Modified
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/main/java/com/example/burprequestsaver/model/ExportTarget.java` - Immutable wrapper for the configured output directory.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/main/java/com/example/burprequestsaver/model/PreparedExport.java` - Immutable pairing of export target, derived filename, and request bytes.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/main/java/com/example/burprequestsaver/export/RawRequestWriter.java` - Byte-preserving file writer with `CREATE_NEW` collision retries.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/test/java/com/example/burprequestsaver/export/RawRequestWriterTest.java` - Regression coverage for file naming, byte fidelity, directory creation, and input validation.

## Decisions Made
- Treat prepared exports as immutable values so later Burp integration can hand the writer one typed payload per request.
- Preserve existing files with `CREATE_NEW` and suffix retries instead of existence pre-checks or overwrite-prone writes.
- Enforce writer-side validation for null filename, null output directory, and empty request bytes before touching disk.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- The TDD RED phase failed at compile time because `RawRequestWriter` did not exist yet, which correctly established the missing implementation.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Phase 1 now has both deterministic filename derivation and collision-safe raw file writing in place.
- Ready for Phase 2 Burp context-menu wiring to pass selected requests into the prepared export pipeline.

## Self-Check: PASSED
