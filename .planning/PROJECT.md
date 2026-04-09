# Burp Request Saver

## What This Is

Burp Request Saver is a Java Burp Suite extension for Burp users who want to quickly export selected HTTP requests to disk. A user sends requests to the extension from a context menu action, and the extension immediately saves each selected request into a configured folder using auto-generated filenames.

## Core Value

A Burp user can reliably save selected raw HTTP requests to disk with almost no friction.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Burp users can right-click one or more requests and send them to the extension from a context menu action
- [ ] The extension saves each selected request immediately without prompting for a filename
- [ ] The extension writes the raw HTTP request to disk
- [ ] The extension uses auto-generated filenames based on host and path
- [ ] The extension defaults to the Burp project directory when available
- [ ] The extension falls back to a Burp-specific folder under the user's home directory when the Burp project directory is unavailable
- [ ] Users can change the output folder after install
- [ ] Users can change the output format after install

### Out of Scope

- Advanced request transformation before saving — the main value is fast, reliable export of raw requests
- Complex workflow automation or queuing UI — v1 should save immediately from the context menu
- Prompting users for filenames per request — this adds friction and conflicts with the intended workflow

## Context

The extension is intended for general Burp Suite users rather than only a single personal workflow. The interaction model is intentionally minimal: select one or more requests in Burp, invoke the context menu action, and have each request saved right away. Reliability matters more than richer UI or broader feature scope for the first version.

The saved output should preserve raw HTTP requests so the files are easy to inspect and reuse in other tools. Filename generation should derive from the request host and path while still ensuring uniqueness.

## Constraints

- **Tech stack**: Java Burp extension — the extension must be written in Java
- **Integration**: Burp Suite context menu workflow — saving starts from a user action inside Burp
- **Usability**: Immediate save behavior — users should not be interrupted by filename prompts
- **Storage**: Configurable output location — default should prefer Burp project directory, with user-home fallback

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Use a context menu action as the main trigger | Fits naturally into Burp request workflows and keeps v1 simple | — Pending |
| Save raw HTTP immediately with auto-generated filenames | Minimizes friction and preserves request fidelity | — Pending |
| Default to Burp project directory, then fall back to user home | Keeps files near project context when possible while remaining robust | — Pending |
| Support changing output folder and output format in settings | Users need some post-install configuration without expanding v1 too far | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-09 after initialization*
