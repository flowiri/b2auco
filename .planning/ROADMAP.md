# Roadmap: Burp Request Saver

## Overview

Burp Request Saver reaches v1 by delivering a tight, reliability-first export workflow: first make saved request files correct and collision-safe, then connect that engine to Burp’s context-menu workflow with responsive batch feedback, then make output location selection robust by handling project-aware defaults and fallbacks, and finally let users manage global and per-project save locations from a lightweight settings surface. This roadmap follows the v1 scope in `REQUIREMENTS.md`; broader output-format configurability mentioned in `PROJECT.md` is not yet defined as a v1 requirement and is therefore left out of these phases.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Export File Semantics** - Users get faithful raw request files with safe, predictable filenames.
- [ ] **Phase 2: Burp Save Workflow** - Users can save one or many requests directly from Burp without interrupting their workflow.
- [ ] **Phase 3: Output Location Defaults** - Users get the right default save location whether Burp has project context or not.
- [ ] **Phase 4: Folder Settings & Overrides** - Users can review and change where exports are saved at global and project scope.

## Phase Details

### Phase 1: Export File Semantics
**Goal**: Users receive correct raw request files on disk with predictable extensions, host/path-based names, and no silent overwrites.
**Depends on**: Nothing (first phase)
**Requirements**: OUT-01, OUT-02, OUT-03, OUT-04, OUT-05, QUAL-03
**Success Criteria** (what must be TRUE):
  1. User receives one saved file for each request passed into the export pipeline.
  2. User can open a saved file and see the HTTP request preserved as displayed in Burp.
  3. User receives exported files with a `.txt` extension and filenames derived from the request host and path.
  4. User does not lose earlier exports when multiple requests would otherwise generate the same filename.
**Plans**: 2 plans
Plans:
- [ ] 01-01-PLAN.md — Bootstrap the Java test harness and lock deterministic host/path filename semantics.
- [ ] 01-02-PLAN.md — Implement collision-safe raw request writing with byte-fidelity tests.

### Phase 2: Burp Save Workflow
**Goal**: Users can trigger immediate, responsive request export from Burp and understand the result of each batch save.
**Depends on**: Phase 1
**Requirements**: TRIG-01, TRIG-02, TRIG-03, QUAL-01, QUAL-02
**Success Criteria** (what must be TRUE):
  1. User can right-click selected Burp requests and invoke a save action from the context menu.
  2. User can save a batch of selected requests in one action instead of repeating the workflow per request.
  3. User is not prompted to name files before the export completes.
  4. User can continue using Burp while a save action runs and receives clear feedback on full success, partial success, or failure.
**Plans**: TBD

### Phase 3: Output Location Defaults
**Goal**: Users get a reliable export destination automatically, with project-aware behavior when Burp can provide project context and a safe fallback when it cannot.
**Depends on**: Phase 2
**Requirements**: OUT-06, CONF-01, CONF-02
**Success Criteria** (what must be TRUE):
  1. User receives exported files in the currently resolved output folder without needing to choose a folder during each save.
  2. User working in a Burp project with a project directory available gets that project-aware location as the default export folder.
  3. User working without a project directory still gets exports saved to a Burp-specific folder under the user home directory.
**Plans**: TBD

### Phase 4: Folder Settings & Overrides
**Goal**: Users can manage export destination settings after installation, including a reusable global default and a current-project override.
**Depends on**: Phase 3
**Requirements**: CONF-03, CONF-04, CONF-05
**Success Criteria** (what must be TRUE):
  1. User can open a settings view and change the output folder after installation.
  2. User can save a global default output folder that future exports use when no project override is set.
  3. User can set a different output folder for the current Burp project without changing the global default.
  4. User can verify through export behavior that project-specific overrides take precedence over the global default for that project.
**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 1.1 → 1.2 → 2 → 2.1 → 3 → 4

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Export File Semantics | 0/2 | Planned | - |
| 2. Burp Save Workflow | 0/TBD | Not started | - |
| 3. Output Location Defaults | 0/TBD | Not started | - |
| 4. Folder Settings & Overrides | 0/TBD | Not started | - |
