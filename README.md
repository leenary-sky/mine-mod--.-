# Minecraft Restrictions

[🇷🇺 Русский](README.ru.md) | [🇬🇧 English](README.md)

**Minecraft Java 26.3 · Fabric**

A challenge mod that turns the video's rules into gameplay restrictions. Once a trigger activates, its restriction is **permanent for that player in that world**. Leaving the world, restarting the game, or dying does not reset it.

## Challenge rules

1. **Obtain an iron ingot** → sprinting is forbidden.
2. **Obtain a diamond** → shields are forbidden.
3. **Enter the Nether for the first time** → armor is forbidden.
4. **Discover a Nether Fortress** → bows are forbidden.
5. **Enter the End for the first time** → placing blocks is forbidden.
6. Goal: defeat the Ender Dragon.

## Visual feedback

- Russian notifications when each restriction activates.
- Active restrictions are shown in the action bar.
- Armor is removed when the armor restriction activates.

## Important details

- Iron and diamond triggers activate when the item is in the inventory; smelting iron or mining a diamond with a particular tool is not required.
- Fortress detection looks for characteristic Nether Brick blocks near the player. This is a practical recording-friendly trigger, but player-built blocks could also trigger it.
- “No blocks” means no **placing** blocks; breaking blocks remains allowed.
- Progress is stored in persistent player scoreboard tags.

## Build

Java 25 and Gradle are required. From the project root, run:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The JAR will be created in `build/libs/`. Install Fabric Loader for Minecraft 26.3 and a compatible Fabric API version to run it.
