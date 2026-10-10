# Ghost Partner — manual test checklist

Run with exactly two players on the same modded server.

## Rescue success
1. Start a world and connect both players.
2. Kill one player.
3. Confirm the dead player becomes a spectator ghost and the survivor sees the rescue HUD.
4. Move the survivor to the death point; confirm the arrow and distance point toward it.
5. Hold Shift without moving or taking damage for the configured ritual duration.
6. Confirm the ghost returns at the beacon with configured health and brief Resistance, and the HUD disappears.

## Ritual cancellation
- Start holding Shift, then move: progress should reset.
- Start holding Shift, then take damage: progress should reset.
- Leave the beacon radius: progress should reset.
- Disconnect the ghost: ritual must not silently complete while the ghost is offline.

## Failure and reset
- Let the timer expire: both players should be placed in spectator mode and see the loss HUD.
- While rescue is active, kill the surviving player: the round should be lost.
- As an operator, run `/ghostpartner status` to inspect state and `/ghostpartner reset` to clear the test round.

## Not yet implemented
- Final custom ghost player model and animations.
- Simple Voice Chat spatial/filter integration (the config flag is currently only a placeholder).
- Automated in-game integration tests.

A successful Gradle build is necessary but does not replace these multiplayer tests.
