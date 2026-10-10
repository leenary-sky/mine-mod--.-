# Ghost Partner — architecture and visual hand-off

## Game rules (server-authoritative)
- `GhostPartnerMod`: round lifecycle, death handling, automatic ghost mode, timer, failure, ritual, resurrection, commands, compass distribution, and server-side ghost-body orbit.
- `GhostPartnerConfig`: gameplay tuning values; `ghostRadius` caps the synced camera/body orbit distance.
- `RoundHudPayload`: read-only round snapshot sent from server to clients, including the rescue target and allowed orbit radius.
- `GhostOrbitDistancePayload`: client request for a new zoom distance. The server accepts it only from the active ghost and clamps it to the configured limit.
- The client never decides whether a ritual succeeds or a round is lost.

## Ghost camera and body
- The local ghost camera orbits the surviving player using ghost yaw/pitch; camera smoothing is based on elapsed frame time rather than a fixed per-frame fraction.
- The local player entity is moved around the same horizontal orbit so the client requests chunks near the detached camera instead of a stale respawn position. Its height follows the survivor's position/jump; camera pitch only changes the camera height.
- The server applies the same orbit in-place through ordinary player position updates. Hard teleports are reserved for respawn, dimension changes, or a body that has drifted far away.
- The actual ghost uses spectator mode. The temporary soul layer is deliberately rendered only on the ghost's client; it must not be used as authority for the round.

## Rescue compass UI
- The survivor receives a vanilla compass when the round begins. While held, the client HUD shows the round timer, direction/distance to the beacon, and ritual progress.
- `GhostCompassUseMixin` turns right-click on that compass into `GhostCompassScreen`; the screen is informational and does not pause the game.
- A custom registered item/texture can replace the vanilla compass later without changing round-state logic.

## Temporary HUD
The current layout displays:
- rescue timer;
- direction arrow and distance to the death beacon;
- ritual progress and Shift instruction;
- ghost role and round-loss states.

Replace the drawing/layout without changing `GhostPartnerMod` or the payload contract.

## Blockbench model hand-off
Put the editable Blockbench source in `blockbench/ghost_partner.bbmodel`. Export runtime assets only after choosing a renderer:
- texture: `assets/ghost_partner/textures/entity/ghost.png`;
- runtime model/renderer adapter: client-only code under `com.kirane.ghostpartner.client`;
- animation definitions/source: keep beside the Blockbench project and document the export pipeline.

A `.bbmodel` is the editable source project, not automatically a runtime Minecraft entity model. Do not add a renderer dependency until the model format and animation system are chosen. The model integration must remain client-side and must not alter round rules.
