<!-- GSD:project-start source:PROJECT.md -->
## Project

**b2auco**

b2auco is a Java Burp Suite extension for Burp users who want to quickly export selected HTTP requests to disk. A user sends requests to the extension from a context menu action, and the extension immediately saves each selected request into a configured folder using auto-generated filenames.

**Core Value:** A Burp user can reliably save selected raw HTTP requests to disk with almost no friction.

### Constraints

- **Tech stack**: Java Burp extension — the extension must be written in Java
- **Integration**: Burp Suite context menu workflow — saving starts from a user action inside Burp
- **Usability**: Immediate save behavior — users should not be interrupted by filename prompts
- **Storage**: Configurable output location — fallback can prefer the Burp project directory when available, with user-home fallback; saved folder settings are split between a user-wide global default and a project-scoped current-project override
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Framework
| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Java | 21 | Extension runtime language | PortSwigger explicitly recommends Java for new extensions, and Burp supports extensions written in Java 21 or lower. For a small context-menu exporter, Java keeps integration direct and avoids cross-language bridge issues. | HIGH |
| Burp Montoya API | 2026.2 | Official Burp extension API | Use Montoya for all new work. PortSwigger strongly recommends it for new extensions, and the legacy Extender API is no longer actively maintained. Montoya directly supports context-menu registration, persistence, and settings-panel integration, which exactly matches this project. | HIGH |
| Swing | JDK-bundled | Context-menu item and minimal settings UI | Burp’s official Montoya examples use Swing `JMenuItem` for context-menu actions. For this extension, Swing is enough: one menu action and a very small settings surface. Pulling in a heavier UI toolkit would add complexity with no benefit. | HIGH |
### Persistence and Storage
| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Montoya `Preferences` | Bundled with Montoya 2026.2 | Persist user-wide settings across reloads and Burp restarts | This is the right place for install-level settings such as the global default output folder and any future user-wide preferences. PortSwigger documents `preferences()` as Java-preferences-backed storage that survives extension reloads and Burp restarts. | HIGH |
| Montoya `extensionData()` | Bundled with Montoya 2026.2 | Persist project-scoped state for the current Burp project | Use this for the current-project override folder and any future state that should live with the open Burp project. PortSwigger documents it as project-backed when a project file exists, but in-memory only when no project is open, so it should not be the primary home for durable user-wide settings. | HIGH |
| Java NIO (`java.nio.file`) | JDK 21 | Create directories and write raw HTTP request files to disk | NIO is the standard, no-dependency way to safely normalize paths, create missing folders, and write bytes atomically enough for a utility extension. This project does not need Apache Commons IO or similar wrappers. | HIGH |
### Build and Packaging
| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Gradle | 8.x current line | Build the extension JAR | PortSwigger’s starter-project docs are Gradle-based, so using Gradle matches the official path and minimizes setup drift from current Burp docs. For a single-module Java extension, Gradle is simpler than introducing Maven just to package one JAR. | MEDIUM |
| Gradle Shadow Plugin | 8.x current line, only if needed | Build a fat JAR when external libraries are added | For v1, you can likely ship a plain JAR because the core stack is Java + Montoya only. Add Shadow only if you later introduce third-party dependencies that must be bundled into the extension artifact. | MEDIUM |
### Supporting Libraries
| Library | Version | Purpose | When to Use | Confidence |
|---------|---------|---------|-------------|------------|
| SLF4J API + simple backend | 2.0.x, optional | Internal structured logging during development | Only add this if Burp output/error logging becomes too limited for debugging. For v1, Montoya’s logging API is usually sufficient. | LOW |
| JUnit Jupiter | 5.12.x current line | Unit tests for filename generation, path resolution, and settings serialization | Use for pure Java logic that does not require Burp. Keep Burp integration thin and test the logic around it. | MEDIUM |
| AssertJ | 3.27.x current line, optional | Cleaner assertions in unit tests | Helpful but optional. Use it if tests become verbose; skip if you want the smallest possible test stack. | LOW |
## Prescriptive Implementation Choices
### 1. Use Montoya context-menu APIs directly
- `montoyaApi.userInterface().registerContextMenuItemsProvider(...)`
- `ContextMenuEvent.selectedRequestResponses()`
### 2. Split persistence by scope
- store the global default folder and other user-wide settings in `preferences()`
- store the current-project override in `extensionData()`
- keep future install-level preferences out of project-scoped storage
### 3. Use Java NIO for file output
### 4. Keep the UI minimal
- context-menu action for export
- a small Montoya `SettingsPanel` for folder and format settings
## Alternatives Considered
| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Burp API | Montoya API 2026.2 | Legacy Extender API | PortSwigger says the legacy API is no longer actively maintained. Starting greenfield on it creates avoidable migration debt. |
| Persistence | Montoya `preferences()` + `extensionData()` split by scope | `preferences()` only | A split model matches the fixed behavior: the global default is user-wide, while the current-project override is project-scoped. Keeping both in `preferences()` makes the project override depend on synthetic identity keys instead of Burp project-backed state. |
| File I/O | Java NIO | Apache Commons IO | Adds a dependency for functionality the JDK already provides well. Not justified for this extension. |
| UI | Swing + Montoya SettingsPanel | Custom Burp tab first | A full tab is unnecessary for a one-action exporter with a tiny config surface. Build the simplest possible UI first. |
| Build tool | Gradle | Maven | Maven would work, but PortSwigger’s current starter flow is Gradle-based, so Gradle is the least-friction path for a new extension. |
| Language | Java | Kotlin | Kotlin is viable, but the requirement is Java, and the official examples/recommendations for new extensions are centered on Java. Avoid extra language/tooling variability unless Kotlin delivers a specific payoff. |
## What NOT to Use
### Do not use the legacy Extender API for a new project
### Do not put primary user settings only in project-scoped storage
### Do not add heavyweight UI frameworks
### Do not add filesystem helper libraries unless a concrete need appears
### Do not treat raw HTTP requests as ordinary text files internally
## Installation
# Core
# Dependency to include in build.gradle
# Optional test dependencies
# optional
## Recommended Minimal Stack for v1
- Java 21
- Gradle
- `net.portswigger.burp.extensions:montoya-api:2026.2`
- Swing for menu/settings UI
- Montoya `preferences()` for durable user-wide settings
- Montoya `extensionData()` for current-project override state
- Java NIO for directory creation and raw request file writes
- JUnit 5 for pure-logic tests
## Sources
- PortSwigger, Creating Burp extensions: https://portswigger.net/burp/documentation/desktop/extensions/creating
- PortSwigger, Starter project setup: https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/set-up/starter-project
- PortSwigger, Writing your first Burp Suite extension: https://portswigger.net/burp/documentation/desktop/extend-burp/extensions/creating/first-extension
- Montoya Javadoc, `ContextMenuEvent`: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/ui/contextmenu/ContextMenuEvent.html
- Montoya Javadoc, `UserInterface`: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/ui/UserInterface.html
- Montoya Javadoc, `Persistence`: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/persistence/Persistence.html
- Montoya Javadoc, `PersistedObject`: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/persistence/PersistedObject.html
- Montoya Javadoc, `Preferences`: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/persistence/Preferences.html
- Montoya Javadoc, `SettingsPanel`: https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/ui/settings/SettingsPanel.html
- PortSwigger Montoya API repository README: https://github.com/PortSwigger/burp-extensions-montoya-api
- Maven Central artifact page: https://central.sonatype.com/artifact/net.portswigger.burp.extensions/montoya-api/2026.2
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->
## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, or `.github/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
