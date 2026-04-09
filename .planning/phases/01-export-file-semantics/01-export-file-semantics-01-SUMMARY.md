---
phase: 01-export-file-semantics
plan: 01
subsystem: testing
tags: [java, gradle, junit, montoya, filename-policy]
requires: []
provides:
  - Java 21 Gradle wrapper build for the Burp extension repo
  - Typed filename contract with ExportFileName base stem and final filename accessors
  - Deterministic host/path filename derivation for D-01 through D-04
affects: [phase-1-raw-request-writing, export-pipeline, burp-save-workflow]
tech-stack:
  added: [Gradle 8.14.3 wrapper, Java plugin, Montoya API 2026.2, JUnit Jupiter 6.0.3]
  patterns: [pure Java filename policy, TDD red-green commits, typed filename return object]
key-files:
  created:
    - settings.gradle
    - build.gradle
    - gradlew
    - gradlew.bat
    - gradle/wrapper/gradle-wrapper.jar
    - gradle/wrapper/gradle-wrapper.properties
    - src/main/java/com/example/burprequestsaver/model/ExportFileName.java
    - src/main/java/com/example/burprequestsaver/naming/FilenamePolicy.java
  modified:
    - .gitignore
    - src/test/java/com/example/burprequestsaver/naming/FilenamePolicyTest.java
key-decisions:
  - "Use a small ExportFileName value object so later export code can reuse both the base stem and final .txt filename."
  - "Sanitize host and path components with character replacement and separator collapse before appending the explicit root/path segment."
patterns-established:
  - "Filename derivation pattern: host plus flattened query-free path yields a sanitized base stem, then .txt is appended."
  - "Build pattern: use the checked-in Gradle wrapper so all future phase verification runs through ./gradlew."
requirements-completed: [OUT-03, OUT-04]
duration: 6 min
completed: 2026-04-09
---

# Phase 1 Plan 1: Bootstrap the Java test harness and lock deterministic host/path filename semantics Summary

**Java 21 Gradle wrapper bootstrap with a typed filename policy that turns host and query-free path inputs into stable sanitized `.txt` export names.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-09T16:25:26Z
- **Completed:** 2026-04-09T16:32:20Z
- **Tasks:** 3
- **Files modified:** 10

## Accomplishments
- Bootstrapped a runnable Java 21 Gradle project with the Montoya API and JUnit through the checked-in wrapper.
- Locked the filename contract in tests and a typed `ExportFileName` model before implementing derivation logic.
- Implemented `FilenamePolicy` so root paths, flattened paths, query stripping, and filename sanitization behave deterministically.

## Task Commits

Each task was committed atomically:

1. **Task 1: Bootstrap the Java 21 and JUnit execution harness for Phase 1** - `d4d30bc` (chore)
2. **Task 2: Lock the filename contract in tests before implementation** - `f548a18` (test)
3. **Task 3: Implement the deterministic filename policy** - `f4d5d38` (feat)

## Files Created/Modified
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/settings.gradle` - Defines the single-module Gradle root project name.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/build.gradle` - Configures Java 21, Montoya, JUnit, and JUnit Platform execution.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/gradlew` - Unix Gradle wrapper entrypoint.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/gradlew.bat` - Windows Gradle wrapper entrypoint.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/gradle/wrapper/gradle-wrapper.jar` - Wrapper bootstrap binary.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/gradle/wrapper/gradle-wrapper.properties` - Pins the wrapper distribution URL.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/.gitignore` - Ignores generated Gradle state and build outputs.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/main/java/com/example/burprequestsaver/model/ExportFileName.java` - Immutable value object exposing `baseStem()` and `finalFileName()`.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/main/java/com/example/burprequestsaver/naming/FilenamePolicy.java` - Host/path to safe `.txt` filename derivation logic.
- `C:/Users/Lenovo/Desktop/Engineering/b2auco/src/test/java/com/example/burprequestsaver/naming/FilenamePolicyTest.java` - Regression tests for D-01 through D-04 and root/sanitization edge cases.

## Decisions Made
- Used `ExportFileName` instead of raw strings so downstream export-writing code can reuse both the normalized base stem and the final filename.
- Kept filename logic pure Java with no Burp coupling so future phases can compose it into the export pipeline cleanly.
- Treated root, blank, null, and slash-only paths as the explicit `root` segment to avoid host-only filenames.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added the JUnit Platform launcher runtime dependency**
- **Found during:** Task 1 (Bootstrap the Java 21 and JUnit execution harness for Phase 1)
- **Issue:** `./gradlew test` failed because JUnit 6.0.3 required an aligned platform launcher that Gradle 8.14.3 did not provide by default.
- **Fix:** Added `testRuntimeOnly 'org.junit.platform:junit-platform-launcher:6.0.3'` to `build.gradle`.
- **Files modified:** `C:/Users/Lenovo/Desktop/Engineering/b2auco/build.gradle`
- **Verification:** `./gradlew test --tests '*FilenamePolicyTest' --tests '*FilenamePolicy*'` passed after the dependency was added.
- **Committed in:** `d4d30bc`

**2. [Rule 3 - Blocking] Ignored generated Gradle output directories**
- **Found during:** Task 1 (Bootstrap the Java 21 and JUnit execution harness for Phase 1)
- **Issue:** Running the wrapper generated `.gradle/` and `build/` directories, which would otherwise remain as untracked runtime artifacts.
- **Fix:** Added `.gradle/` and `build/` to `.gitignore`.
- **Files modified:** `C:/Users/Lenovo/Desktop/Engineering/b2auco/.gitignore`
- **Verification:** `git status --short` no longer reported generated build directories after test execution.
- **Committed in:** `d4d30bc`

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both changes were required to make the planned wrapper-based verification repeatable without leaving generated artifacts in the working tree.

## Issues Encountered
- Gradle 8.14.3 plus JUnit Jupiter 6.0.3 initially failed test discovery until the matching `junit-platform-launcher` runtime artifact was added.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Ready for `01-02-PLAN.md`, which can now reuse the checked-in wrapper and the typed filename policy for collision-safe raw request writing.
- No open blockers for continuing Phase 1.

## Self-Check: PASSED
- Found summary file on disk
- Found task commits d4d30bc, f548a18, and f4d5d38 in git history
