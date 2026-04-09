# Requirements: Burp Request Saver

**Defined:** 2026-04-09
**Core Value:** A Burp user can reliably save selected raw HTTP requests to disk with almost no friction.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Export Trigger

- [ ] **TRIG-01**: User can right-click one or more selected Burp requests and invoke a save action from the context menu
- [ ] **TRIG-02**: User can save all selected requests in one action
- [ ] **TRIG-03**: User can save requests immediately without being prompted for a filename

### Export Output

- [ ] **OUT-01**: User receives one saved file per selected request
- [ ] **OUT-02**: User receives the HTTP request saved as displayed in Burp
- [x] **OUT-03**: User receives saved request files with a `.txt` extension
- [x] **OUT-04**: User receives auto-generated filenames derived from request host and path
- [ ] **OUT-05**: User does not lose previous exports when multiple requests would otherwise produce the same filename
- [ ] **OUT-06**: User receives saved files in a configured output folder

### Defaults and Settings

- [ ] **CONF-01**: User gets a project-aware default output folder when a Burp project directory is available
- [ ] **CONF-02**: User gets a Burp-specific folder under the user home directory when a project directory is unavailable
- [ ] **CONF-03**: User can change the output folder after installation
- [ ] **CONF-04**: User can configure a global default output folder
- [ ] **CONF-05**: User can override the output folder for the current Burp project

### Reliability and Feedback

- [ ] **QUAL-01**: User can save requests without Burp becoming unresponsive during export
- [ ] **QUAL-02**: User receives clear feedback when a batch save fully succeeds, partially succeeds, or fails
- [ ] **QUAL-03**: User can trust that raw request exports preserve the request content for reuse outside Burp

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Export Options

- **OPTN-01**: User can choose additional output formats beyond request-as-displayed `.txt`
- **OPTN-02**: User can organize exports into structured subfolders such as by host or date
- **OPTN-03**: User can customize the filename template
- **OPTN-04**: User can export request metadata alongside the raw request

## Out of Scope

| Feature | Reason |
|---------|--------|
| Per-request filename prompt | Conflicts with immediate low-friction save workflow |
| Request transformation before save | v1 should preserve the request rather than mutate it |
| Large custom tab or queue management UI | Adds complexity beyond the core export workflow |
| Cloud/remote storage | Not needed for validating the local export utility |
| Request and response pair export | Adjacent capability, but not core to the current value |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| TRIG-01 | Phase 2 | Pending |
| TRIG-02 | Phase 2 | Pending |
| TRIG-03 | Phase 2 | Pending |
| OUT-01 | Phase 1 | Pending |
| OUT-02 | Phase 1 | Pending |
| OUT-03 | Phase 1 | Complete |
| OUT-04 | Phase 1 | Complete |
| OUT-05 | Phase 1 | Pending |
| OUT-06 | Phase 3 | Pending |
| CONF-01 | Phase 3 | Pending |
| CONF-02 | Phase 3 | Pending |
| CONF-03 | Phase 4 | Pending |
| CONF-04 | Phase 4 | Pending |
| CONF-05 | Phase 4 | Pending |
| QUAL-01 | Phase 2 | Pending |
| QUAL-02 | Phase 2 | Pending |
| QUAL-03 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 17 total
- Mapped to phases: 17
- Unmapped: 0

---
*Requirements defined: 2026-04-09*
*Last updated: 2026-04-09 after roadmap creation*
