# Binge

Binge is a JavaFX movie browser with TMDB artwork, searchable shelves, saved titles, and
YouTube trailer playback. The interface uses a dark orange theme and supports light mode.

## Requirements

- JDK 23 or newer
- Maven 3.9 or newer
- A TMDB API key for posters and backdrops

## Configuration

Set `TMDB_API_KEY` in your environment. Do not add a key to the repository.

PowerShell:

```powershell
$env:TMDB_API_KEY = "your-key"
```

To persist the key for your Windows user:

```powershell
[Environment]::SetEnvironmentVariable("TMDB_API_KEY", "your-key", "User")
```

The app uses Wikipedia poster fallbacks when TMDB is unavailable.

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
