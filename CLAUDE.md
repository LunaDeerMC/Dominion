# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Dominion** is an open-source Minecraft server plugin - a territory anti-grief plugin for high-version Minecraft servers (1.20.1+). It's a Java-based project built with Gradle and designed for Bukkit/Spigot/Paper/Folia server implementations.

**Key Features:**
- Territory/land protection system
- Multi-language support (English, Chinese, Japanese, Russian)
- Extensive API for addon development
- Support for multi-server setups
- Folia (async Minecraft server) compatibility

## Architecture

The project uses a **multi-module Gradle build** with the following structure:

```
Dominion/
├── api/                          # Public API module for addon developers
│   └── src/main/java/cn/lunadeer/dominion/api/
│       ├── DominionAPI.java      # Main API interface
│       ├── dtos/                 # Data Transfer Objects
│       ├── events/               # Event system
│       └── providers/            # Provider interfaces
│
├── core/                         # Main plugin implementation
│   └── src/main/java/cn/lunadeer/dominion/
│       ├── Dominion.java         # Main plugin class
│       ├── cache/                # Caching system
│       ├── commands/             # Command handlers
│       ├── configuration/        # Config management
│       ├── events/               # Event handlers
│       ├── managers/             # Various managers
│       ├── uis/                  # User interfaces (Chest UI, Text UI)
│       └── utils/                # Utilities
│
├── v1_20_1/                      # Version-specific implementations
├── v1_21/                        # (for Minecraft version compatibility)
├── v1_21_9/
│
├── languages/                    # Translation files (YAML format)
│   ├── en_us.yml                 # English translations (reference)
│   ├── zh_cn.yml                 # Simplified Chinese
│   ├── zh_tw.yml                 # Traditional Chinese
│   ├── jp_jp.yml                 # Japanese
│   └── ...                       # Other languages
│
├── docs/                         # Documentation (VuePress)
│   ├── src/notes/api/            # API documentation
│   ├── src/notes/doc/            # User documentation
│   └── package.json              # Node.js dependencies
│
└── build.gradle.kts              # Root build configuration
```

## Common Commands

### Building

```bash
# Build the plugin JAR (lite version - dependencies not bundled)
./gradlew shadowJar

# Build full version (dependencies bundled in JAR)
./gradlew shadowJar -PBuildFull=true

# Clean and build (recommended for development)
./gradlew Clean&Build

# Build API module
cd api && ./gradlew clean build
```

**Output:** JAR files in `build/libs/` directory with naming format: `Dominion-{version}-{lite|full}.jar`

### Testing

**Important:** This project does **not have automated unit tests**. Testing is done manually on Minecraft servers.

- No `src/test/java/` directories exist
- No test framework configured (JUnit, TestNG, etc.)
- Manual testing on development Minecraft servers

### Running

The plugin is designed to be run on a Minecraft server (Paper/Spigot/Folia):
1. Build the JAR using `./gradlew shadowJar`
2. Copy the JAR to your server's `plugins/` directory
3. Start/restart the server
4. Configure via `plugins/Dominion/config.yml`

### Documentation

```bash
# Run documentation site locally
cd docs && npm run docs:dev

# Build documentation for deployment
cd docs && npm run docs:build
```

## Key Configuration Files

### Gradle Configuration
- **`build.gradle.kts`**: Main build configuration
  - Java 17 toolchain
  - ShadowJar plugin for fat JARs
  - Version auto-increment based on git branch
  - Hangar publishing plugin
  - Multi-module project setup

- **`settings.gradle.kts`**: Defines modules: `api`, `core`, `v1_20_1`, `v1_21`, `v1_21_9`

### Plugin Configuration
- **`core/src/main/resources/plugin.yml`**: Plugin metadata
  - Main class: `cn.lunadeer.dominion.Dominion`
  - API version: 1.20
  - Folia supported: true
  - Soft dependencies: Vault, PlaceholderAPI, WorldGuard
  - Permissions: `dominion.admin`, `dominion.default`

### Version Management
- **`version.properties`**: Auto-generated file tracking version suffixes
  - Format: `alpha.X` for dev branches, `beta` for main branch
  - Current version: 4.7.2-alpha.12

## Development Workflow

### Building the Project
1. Ensure you have JDK 21+ and Gradle installed
2. Clone the repository:
   ```bash
   git clone https://github.com/LunaDeerMC/Dominion.git
   cd Dominion
   git submodule update --init --recursive
   ```
3. Build the plugin JAR:
   ```bash
   ./gradlew shadowJar
   ```

### Code Style
- Follow existing code patterns in the `core/` and `api/` modules
- Use clear and concise commit messages
- IntelliJ IDEA is the recommended IDE (JetBrains Open Source License)

### Translations
- Translation files are in `languages/` directory
- Use `zh-cn.yml` as the reference for updates
- Add your name in the header comment of translated files
- Direct file edits are preferred over Crowdin

### Documentation
- Documentation contributions go in `docs/`
- Follow the structure of the `zh-cn` directory for new languages

## Integration Points

### API
The `api/` module provides hooks and utilities for addon development:
- Refer to: https://dominion.lunadeer.cn/en/notes/api/
- Key interfaces: `DominionAPI`, event listeners, data operations, custom flags

### External Integrations
- **bStats**: Plugin analytics integration
- **Crowdin**: Translation sync (direct file edits preferred)
- **Vault**: Economy/permissions integration
- **PlaceholderAPI**: Placeholder support
- **WorldGuard**: Optional integration

## Build Variants

### Lite Version (Default)
```bash
./gradlew shadowJar
```
- Dependencies are `compileOnly`
- Users must install required plugins separately
- Smaller JAR file size

### Full Version
```bash
./gradlew shadowJar -PBuildFull=true
```
- Dependencies are bundled in the JAR
- Self-contained deployment
- Larger JAR file size

## Publishing

The project is published to:
- **Hangar** (PaperMC): https://hangar.papermc.io/zhangyuheng/Dominion
- **Modrinth**: https://modrinth.com/plugin/lunadeer-dominion
- **SpigotMC**: https://www.spigotmc.org/resources/dominion.119514/

API is published to Maven Central.

## CI/CD

### GitHub Workflows
- **`.github/workflows/check.yml`**: PR build verification
- **`.github/workflows/release.yml`**: Release automation (triggers on git tags)
- **`docs/.github/workflows/wiki.yml`**: Documentation deployment to GitHub Pages

## Important Notes

### No Automated Tests
This project does not have automated unit tests. All testing is done manually on Minecraft servers.

### Version Compatibility
- Supports Minecraft 1.20.1+ (Bukkit, Spigot, Paper, Folia)
- Strongly recommend Paper or its forks (Purpur) for optimal performance
- Version-specific implementations in `v1_20_1/`, `v1_21/`, `v1_21_9/` directories

### Database Support
- PostgreSQL, MySQL (via HikariCP)
- Configuration in `config.yml`

### Multi-Server Support
- Supports multi-server setups with shared database
- Configuration in `config.yml` under multi-server section

## Getting Help

- **GitHub Issues**: https://github.com/LunaDeerMC/Dominion/issues
- **API Documentation**: https://dominion.lunadeer.cn/en/notes/api/
- **User Documentation**: https://dominion.lunadeer.cn/
- **Contribution Guidelines**: See [CONTRIBUTING.md](CONTRIBUTING.md)

## Quick Reference

| Task | Command |
|------|---------|
| Build plugin (lite) | `./gradlew shadowJar` |
| Build plugin (full) | `./gradlew shadowJar -PBuildFull=true` |
| Clean & build | `./gradlew Clean&Build` |
| Build API | `cd api && ./gradlew clean build` |
| Run docs locally | `cd docs && npm run docs:dev` |
| View all tasks | `./gradlew tasks` |
