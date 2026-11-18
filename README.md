# Binge

Binge is a JavaFX movie browser with TMDB artwork, searchable shelves, saved titles, and
YouTube trailer playback. The interface uses a dark orange theme and supports light mode.

## Requirements

- JDK 23 or newer
- Maven 3.9 or newer
- A TMDB API read-access token (recommended), or a legacy v3 API key, for posters and backdrops

## Configuration

Set `TMDB_ACCESS_TOKEN` (recommended) or `TMDB_API_KEY` in your environment. Do not add a credential to the repository.

PowerShell:

```powershell
$env:TMDB_ACCESS_TOKEN = "your-read-access-token"
```

To persist the key for your Windows user:

```powershell
[Environment]::SetEnvironmentVariable("TMDB_ACCESS_TOKEN", "your-read-access-token", "User")
```

The app first requests TMDB artwork, then falls back to a Wikipedia poster if TMDB is unavailable,
returns no matching poster, or no TMDB credential is configured.

## Build and run

```powershell
cd binge
mvn clean test
mvn javafx:run
```

In VS Code, run **Binge Launch** from the task list. The included PowerShell task loads a
persisted Windows user environment variable before starting Maven.

## Project layout

- `binge/src/main/java` - JavaFX application, data, player, and UI code
- `binge/src/main/resources` - catalog, stylesheet, and player HTML
- `.vscode` - portable local build and launch tasks

User preferences are stored outside the repository in `~/.binge/library.json`.

## Publishing

Generated build output, local environment files, IDE metadata, and credentials are excluded by
`.gitignore`. Review new files before committing and never add an API key to Git.
