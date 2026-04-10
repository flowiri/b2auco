# b2auco

b2auco is a Java Burp Suite extension for saving selected HTTP requests to disk.

It adds:
- a `b2auco` suite tab in Burp for folder settings
- a `b2auco -> Save requests` context-menu action for exporting requests

## What it does

Today, the extension can:
- export selected raw HTTP requests from Burp to files on disk
- generate filenames automatically so Burp does not need to ask for a filename each time
- let you configure a global default export folder stored as a user-wide setting
- let you configure a Current project override stored as project-scoped state for the current Burp project
- fall back to a default export folder when no saved setting exists

## Requirements

- Java 21
- Burp Suite

This repo includes the Gradle wrapper, so you can build with `./gradlew`.

## Build

From the repository root, run:

```bash
./gradlew jar
```

Built jar:

```text
build/libs/b2auco-0.1.0-SNAPSHOT.jar
```

## Load the extension in Burp

1. Build the jar.
2. Open Burp Suite.
3. Go to the extensions area in Burp.
4. Add a new extension.
5. Choose the built jar file:

```text
build/libs/b2auco-0.1.0-SNAPSHOT.jar
```

6. Load it.
7. Confirm Burp shows a suite tab named `b2auco`.

## Use it

### Export requests from the context menu

1. In Burp, select one or more HTTP requests.
2. Open the context menu.
3. Choose `b2auco`.
4. Click `Save requests`.
5. The extension writes the selected requests to the current export folder.

The extension saves immediately. It does not prompt for a filename for each request.

## Folder settings

The `b2auco` tab contains the export folder settings.

### Global default folder

Use **Global default folder** to choose the folder used for normal exports. This setting is user-wide, so it is available across Burp restarts and across Burp projects on the same machine.

### Current project override

Use **Current project override** when you want one Burp project to export to a different folder.

Enable **Override folder for this project only** to set a project-specific folder. This override is project-scoped and persists with the current Burp project.

## Folder precedence

At a high level, the extension resolves the export folder in this order:

1. **Current project override**
2. **Global default folder**
3. fallback default folder

## Fallback default folder

If no saved folder setting applies, b2auco falls back to:

- a project-local `.b2auco/exports` folder when a Burp project directory is available, or
- `~/.b2auco/exports` under the current user home directory when no project directory is available

So the effective order is: project override, then global default, then fallback default.

## Notes

- The extension is implemented in Java and uses the Burp Montoya API.
- The current artifact version in this repo is `0.1.0-SNAPSHOT`.
