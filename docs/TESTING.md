# Ghost Partner — manual test checklist

Run with exactly two players on the same modded server. Install the mod on both clients and the server.

## Death, respawn, and ghost view
1. Start a fresh world and connect both players.
2. Kill one player. Confirm the survivor gets a rescue compass and sees the timer.
3. Confirm the dead player is automatically respawned without clicking the vanilla death-screen button.
4. Confirm the ghost enters spectator mode, has no normal HUD/body visible, and sees the survivor from an orbiting camera.
5. Move the mouse left/right: the ghost body should orbit horizontally around the survivor and its head should keep facing the survivor.
6. Move the mouse up/down: only the camera height should change; the body should continue tracking horizontally.
7. Have the survivor jump: the server-side ghost body should rise/fall with them.
8. Zoom with the mouse wheel: camera and ghost-body orbit should remain close, including after the server sends a HUD update.
9. Walk/sprint with the survivor and check that the camera follows without persistent snapping or missing chunks. Repeat with a low render distance and a wall between players.
10. Confirm the temporary soul visual is visible to the ghost client only.

## Compass and rescue success
1. Select the rescue compass and hold it: confirm the HUD shows timer, direction, distance, and ritual state.
2. Right-click the compass: confirm the non-pausing rescue panel opens, shows the death coordinates, and closes with Esc.
3. Move the survivor to the death point; confirm the direction/distance reach zero.
4. Hold Shift without moving or taking damage for the configured ritual duration.
5. Confirm the ghost returns at the beacon in Survival with configured health and brief Resistance, and the rescue HUD disappears.

## Ritual cancellation
- Start holding Shift, then move: progress should reset.
- Start holding Shift, then take damage: progress should reset.
- Leave the beacon radius: progress should reset.
- Disconnect the ghost: ritual must not silently complete while the ghost is offline.

## Failure and reset
- Let the timer expire: both players should be placed in spectator mode and see the loss state.
- While rescue is active, kill the surviving player: the round should be lost.
- As an operator, run `/ghostpartner status` to inspect state and `/ghostpartner reset` to clear the test round.

## Not yet implemented
- Final custom ghost player model and animations.
- A separately registered compass item/texture (the current UI reuses the vanilla compass).
- Simple Voice Chat spatial/filter integration.
- Automated in-game integration tests.

A successful Gradle build is necessary but does not replace these multiplayer tests.
