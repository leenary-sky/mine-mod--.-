# Ghost Partner — architecture and visual hand-off

## Game rules (server-authoritative)
- `GhostPartnerMod`: round lifecycle, death handling, timer, failure, ritual, resurrection, commands.
- `GhostPartnerConfig`: gameplay tuning values.
- `RoundHudPayload`: read-only snapshot sent from server to clients.
- Client HUD must never decide whether a ritual succeeds or a round is lost.

## Temporary HUD
The current `GhostPartnerClient` is a placeholder layout:
- rescue timer;
- direction arrow and distance to the death beacon;
- ritual progress and Shift instruction;
- ghost role and round-loss states.

Replace the drawing/layout without changing `GhostPartnerMod` or the payload contract.

## Blockbench model hand-off
Put the editable Blockbench source in `blockbench/ghost_partner.bbmodel`.
Export runtime assets only after choosing a renderer:
- texture: `assets/ghost_partner/textures/entity/ghost.png`;
- runtime model/renderer adapter: client-only code under `com.kirane.ghostpartner.client`;
- animation definitions/source: keep beside the Blockbench project and document the export pipeline.

A `.bbmodel` is the editable source project, not automatically a runtime Minecraft entity model. Do not add a renderer dependency until the model format and animation system are chosen. The model integration must remain client-side and must not alter round rules.
