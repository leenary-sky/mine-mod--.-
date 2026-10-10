# Ghost Partner — Fabric 26.3

[🇷🇺 Русский](README.ru.md) | [🇬🇧 English](README.md)

A two-player co-op challenge. After the first death, the dead player becomes an invisible spectator ghost. The survivor has four minutes to reach the death location and complete a short rescue ritual.

> This is an early build. The listed mechanics are implemented in source, but still need an in-game test with a dedicated server and two clients before recording.

## Implemented

- Starts a round after one of exactly two connected players dies; round state is persisted in world command storage.
- Automatically respawns the ghost through a brief blackout instead of leaving the vanilla death screen open.
- The ghost uses spectator mode. Its server-side player body follows a horizontal orbit around the survivor, tracks the survivor's jump height, and smoothly turns its head toward them.
- The ghost camera orbits the survivor with frame-time-based smoothing. Mouse-wheel zoom is synchronized to the server so the body stays near the camera.
- A small soul model is rendered only on the ghost's client.
- The survivor receives a rescue compass. Holding it displays the direction, distance, timer, and ritual progress; right-clicking it opens a panel with the death coordinates.
- Soul-particle beacon, protection against nearby dropped loot despawning, Shift-to-rescue ritual, and ritual reset on movement or damage.
- Resurrection with 6 HP and three seconds of Resistance.
- Round loss if the survivor dies or the timer expires.
- Operator commands: `/ghostpartner status`, `/ghostpartner reset`, and `/ghostpartner restart`.

## Not finished / still needs verification

- Final ghost player model, skin, and animations (the current soul is a temporary cube model).
- A separately registered compass item and custom texture; the current panel reuses the vanilla compass.
- Testing camera/body movement on a dedicated server, in single-player, and with network latency.
- Simple Voice Chat integration, final UI art, and automated in-game tests.
- World deletion is intentionally not implemented. The reserved `deleteWorldOnFailure` config property does not delete anything.

## Build

Java 25 and Gradle are required. From the repository root:

```bash
gradle --no-daemon clean build
```

The JAR is written to `build/libs/`. Install Fabric Loader for Minecraft 26.3 and a compatible Fabric API version; install the mod on the server and both clients.

## Manual test

See [`docs/TESTING.md`](docs/TESTING.md). Test automatic respawn, orbit movement at different zoom levels, survivor jumps, the compass panel, and rescue completion.
