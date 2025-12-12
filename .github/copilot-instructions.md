# Copilot Instructions for Dominion

## Project Overview
Dominion is an open-source, future-proof anti-grief plugin for high-version Minecraft servers. It supports Bukkit, Spigot, Paper, and Folia, with a strong recommendation for Paper or its forks for optimal performance. The project is actively developed and includes a modular API for creating addons.

### Key Components
- **Core Plugin**: Located in `core/`, this contains the main functionality of the Dominion plugin.
- **API**: Found in `api/`, this module provides interfaces and utilities for addon development.
- **Documentation**: The `docs/` directory contains user and developer guides, including multi-language support.
- **Language Files**: Translation files are in `languages/` and follow the YAML format.

## Developer Workflows

### Building the Project
1. Ensure you have JDK 21+ and Gradle installed.
2. Clone the repository and initialize submodules:
   ```bash
   git clone https://github.com/LunaDeerMC/Dominion.git
   cd Dominion
   git submodule update --init --recursive
   ```
3. Build the plugin JAR:
   ```bash
   ./gradlew shadowJar
   ```
   The output JAR will be in `build/libs/`.

### Running Tests
- Tests are located in `src/test/java/`.
- Use Gradle to execute tests:
  ```bash
  ./gradlew test
  ```

### Debugging
- Use IntelliJ IDEA for development. The project is optimized for this IDE, with features like code completion and debugging tools.

## Project-Specific Conventions

### Code Style
- Follow existing code patterns in the `core/` and `api/` modules.
- Use clear and concise commit messages.

### Translations
- Translation files are in `languages/`.
- Use `zh-cn.yml` as the reference for updates.
- Add your name in the header comment of translated files.

### Documentation
- Documentation contributions go in `docs/`.
- Follow the structure of the `zh-cn` directory for new languages.

## Integration Points
- **API**: The `api/` module provides hooks and utilities for addon development. Refer to the [API documentation](https://dominion.lunadeer.cn/en/notes/api/).
- **bStats**: The plugin integrates with bStats for analytics. Ensure any new metrics are registered appropriately.
- **Crowdin**: Translations are synced with Crowdin, but direct file edits are preferred.

## Examples

### Adding a New Command
1. Define the command in `plugin.yml`.
2. Implement the command logic in `core/src/main/java/`.
3. Register the command in the plugin's main class.

### Creating an Addon
1. Use the `api/` module as a dependency.
2. Implement the required interfaces.
3. Package and distribute the addon separately.

## Additional Resources
- [Contribution Guidelines](../CONTRIBUTING.md)
- [API Documentation](https://dominion.lunadeer.cn/en/notes/api/)
- [GitHub Issues](https://github.com/LunaDeerMC/Dominion/issues)

For any questions or clarifications, open an issue or refer to the documentation.