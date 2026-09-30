# Minecraft Restrictions

Fabric mod for Minecraft Java 26.3.

Challenge progression:

1. Get iron -> sprinting is disabled.
2. Get diamonds -> shield use is disabled.
3. Enter the Nether -> armor is disabled.
4. Reach a Nether Fortress -> bow use is disabled.
5. Enter the End -> placing blocks is disabled.
6. Kill the Ender Dragon.

Implementation notes:

- Progress is stored using persistent player scoreboard tags, so restrictions survive reconnects and restarts.
- Armor is automatically removed once the Nether restriction activates.
- The fortress trigger detects fortress-specific Nether Brick blocks around the player.
- Block restriction means placing blocks, not breaking them.
- Current restrictions are shown in the action-bar overlay.
- Every new restriction produces a visible notification.

Development target:

- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.160.0+26.3
- Java 25
- Fabric Loom 1.17.x

Open the repository as a Gradle project in IntelliJ IDEA or VS Code. Fabric recommends IntelliJ IDEA for mod development.

The project targets stable 26.3 rather than the 26.4 snapshot branch.

The built JAR is produced in build/libs/.
