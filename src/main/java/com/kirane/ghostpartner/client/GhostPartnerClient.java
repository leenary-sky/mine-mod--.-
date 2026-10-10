package com.kirane.ghostpartner.client;

import com.kirane.ghostpartner.GhostPartnerMod;
import com.kirane.ghostpartner.RoundHudPayload;
import com.kirane.ghostpartner.client.mixin.GhostCameraAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;

/**
 * Temporary, deliberately simple HUD. The layout can be replaced without changing round logic.
 * Art direction and asset files are intentionally kept separate from this renderer.
 */
public final class GhostPartnerClient implements ClientModInitializer {
    private static volatile RoundHudPayload state =
            new RoundHudPayload(false, (byte) 0, 0, BlockPos.ZERO, false, 0, 100);
    private static volatile long transitionStartedAt = 0L;
    private static net.minecraft.client.player.LocalPlayer lastObservedPlayer;
    private static boolean respawnRequested;
    private static final long FADE_IN_MS = 3500L;
    private static final long BLACK_HOLD_MS = 1000L;
    private static final long FADE_OUT_MS = 3500L;
    private static final long TRANSITION_DURATION_MS = FADE_IN_MS + BLACK_HOLD_MS + FADE_OUT_MS;
    private static double orbitDistance = 5.0;
    private static double smoothX = Double.NaN, smoothY = Double.NaN, smoothZ = Double.NaN;

    public static boolean isGhostCameraActive() {
        return state.active() && state.role() == 2;
    }

    public static void adjustOrbitZoom(double scroll) {
        orbitDistance = Mth.clamp(orbitDistance - scroll * 0.8, 2.0, 14.0);
    }

    public static void applyOrbitCamera(net.minecraft.client.Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (!isGhostCameraActive() || mc.player == null || mc.level == null) {
            smoothX = smoothY = smoothZ = Double.NaN;
            return;
        }
        net.minecraft.client.player.LocalPlayer ghost = mc.player;
        net.minecraft.client.player.AbstractClientPlayer target = null;
        for (net.minecraft.client.player.AbstractClientPlayer candidate : mc.level.players()) {
            if (candidate != ghost) { target = candidate; break; }
        }
        if (target == null) return;

        double targetX = target.getX();
        double targetY = target.getY() + target.getBbHeight() * 0.82;
        double targetZ = target.getZ();
        double yaw = Math.toRadians(ghost.getYRot());
        double pitch = Math.toRadians(Mth.clamp(ghost.getXRot(), -75.0f, 75.0f));
        double horizontal = Math.cos(pitch) * orbitDistance;
        double desiredX = targetX - Math.sin(yaw) * horizontal;
        double desiredY = targetY + Math.sin(pitch) * orbitDistance;
        double desiredZ = targetZ + Math.cos(yaw) * horizontal;

        if (Double.isNaN(smoothX)) {
            smoothX = desiredX; smoothY = desiredY; smoothZ = desiredZ;
        } else {
            smoothX += (desiredX - smoothX) * 0.22;
            smoothY += (desiredY - smoothY) * 0.22;
            smoothZ += (desiredZ - smoothZ) * 0.22;
        }
        ((GhostCameraAccess) (Object) camera).ghostPartner$setPosition(new Vec3(smoothX, smoothY, smoothZ));
        double dx = targetX - smoothX, dy = targetY - smoothY, dz = targetZ - smoothZ;
        float lookYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float lookPitch = (float)-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        ((GhostCameraAccess) (Object) camera).ghostPartner$setRotation(lookYaw, lookPitch);
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RoundHudPayload.TYPE,
                (payload, context) -> context.client().execute(() -> state = payload));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && state.active() && state.role() == 2
                    && client.player.getHealth() <= 0.0f) {
                if (transitionStartedAt == 0L) transitionStartedAt = System.currentTimeMillis();
                if (!respawnRequested && System.currentTimeMillis() - transitionStartedAt >= FADE_IN_MS + 100L
                        && client.getConnection() != null) {
                    client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(
                            net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                    respawnRequested = true;
                }
            }
            if (client.player != null && lastObservedPlayer != null && client.player != lastObservedPlayer) {
                respawnRequested = false;
            }
            if (client.player == null) respawnRequested = false;
            lastObservedPlayer = client.player;
        });
        HudElementRegistry.addLast(
                Identifier.parse(GhostPartnerMod.MOD_ID + ":round_hud"),
                GhostPartnerClient::renderHud
        );
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        renderRespawnTransition(graphics, minecraft);

        // The temporary compass acts as the HUD device: no permanent on-screen nagging.
        boolean compassEquipped = minecraft.player.getMainHandItem().is(Items.COMPASS)
                || minecraft.player.getOffhandItem().is(Items.COMPASS);
        if (!compassEquipped) return;

        RoundHudPayload snapshot = state;
        if (snapshot.role() == 3) {
            drawPanel(graphics, minecraft, "РАУНД ПРОИГРАН", "Оператор может сбросить раунд", GhostHudStyle.DANGER);
            return;
        }
        if (!snapshot.active() || snapshot.role() == 0) return;

        int seconds = Math.max(0, snapshot.remainingSeconds());
        String timer = String.format("%02d:%02d", seconds / 60, seconds % 60);
        if (snapshot.role() == 2) {
            drawPanel(graphics, minecraft, "ТЫ — ПРИЗРАК", "Веди напарника к маяку  ·  " + timer, GhostHudStyle.GHOST_ACCENT);
            return;
        }

        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int x = screenWidth / 2 - GhostHudStyle.PANEL_WIDTH / 2;
        int y = GhostHudStyle.TOP_MARGIN;
        int width = GhostHudStyle.PANEL_WIDTH;
        int height = GhostHudStyle.SURVIVOR_PANEL_HEIGHT;
        graphics.fill(x, y, x + width, y + height, GhostHudStyle.PANEL_BACKGROUND);
        graphics.outline(x, y, width, height, GhostHudStyle.PANEL_BORDER);
        graphics.text(minecraft.font, "СПАСЕНИЕ НАПАРНИКА", x + 10, y + 8, GhostHudStyle.ACCENT, false);
        graphics.text(minecraft.font, timer, x + width - 48, y + 8, seconds <= 30 ? GhostHudStyle.TIMER_DANGER : GhostHudStyle.TEXT, false);

        BlockPos target = snapshot.target();
        if (!snapshot.targetInSameDimension()) {
            graphics.text(minecraft.font, "МАЯК В ДРУГОМ ИЗМЕРЕНИИ",
                    x + 10, y + 25, GhostHudStyle.TEXT, false);
        } else {
            double distance = minecraft.player.position().distanceTo(Vec3.atCenterOf(target));
            String direction = directionArrow(minecraft, target);
            graphics.text(minecraft.font, direction + "  МАЯК  ·  " + Math.round(distance) + " м",
                    x + 10, y + 25, GhostHudStyle.TEXT, false);
        }

        int barX = x + 10;
        int barY = y + 45;
        int barWidth = width - 20;
        int barHeight = 8;
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, GhostHudStyle.BAR_BACKGROUND);
        int total = Math.max(1, snapshot.ritualTotalTicks());
        int progress = Mth.clamp(snapshot.ritualTicks(), 0, total);
        int filled = (int) (barWidth * (progress / (float) total));
        if (filled > 0) graphics.fill(barX, barY, barX + filled, barY + barHeight, GhostHudStyle.BAR_FILL);
        String ritual = progress > 0
                ? "РИТУАЛ  " + Math.round(progress * 100.0 / total) + "%"
                : "УДЕРЖИВАЙ SHIFT У МАЯКА";
        graphics.text(minecraft.font, ritual, barX, y + 56, GhostHudStyle.MUTED_TEXT, false);
    }

    private static void renderRespawnTransition(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        long started = transitionStartedAt;
        if (started == 0L) return;
        long elapsed = System.currentTimeMillis() - started;
        if (elapsed >= TRANSITION_DURATION_MS) {
            transitionStartedAt = 0L;
            return;
        }

        float amount;
        if (elapsed <= FADE_IN_MS) {
            amount = elapsed / (float) FADE_IN_MS;
        } else if (elapsed <= FADE_IN_MS + BLACK_HOLD_MS) {
            amount = 1.0f;
        } else {
            amount = 1.0f - (elapsed - FADE_IN_MS - BLACK_HOLD_MS) / (float) FADE_OUT_MS;
        }
        int alpha = Mth.clamp(Math.round(255.0f * amount), 0, 255);
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, (alpha << 24));
    }

    private static String directionArrow(Minecraft minecraft, BlockPos target) {
        double dx = target.getX() + 0.5 - minecraft.player.getX();
        double dz = target.getZ() + 0.5 - minecraft.player.getZ();
        if (dx * dx + dz * dz < 0.25) return "•";
        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double relative = Mth.wrapDegrees((float) (targetYaw - minecraft.player.getYRot()));
        int sector = Math.floorMod((int) Math.round(relative / 45.0), 8);
        return new String[] {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"}[sector];
    }

    private static void drawPanel(GuiGraphicsExtractor graphics, Minecraft minecraft,
                                  String title, String subtitle, int accent) {
        int width = minecraft.getWindow().getGuiScaledWidth();
        int x = width / 2 - GhostHudStyle.PANEL_WIDTH / 2;
        int y = GhostHudStyle.TOP_MARGIN;
        graphics.fill(x, y, x + GhostHudStyle.PANEL_WIDTH, y + GhostHudStyle.COMPACT_PANEL_HEIGHT, GhostHudStyle.PANEL_BACKGROUND);
        graphics.outline(x, y, GhostHudStyle.PANEL_WIDTH, GhostHudStyle.COMPACT_PANEL_HEIGHT, accent);
        graphics.text(minecraft.font, title, x + 10, y + 8, accent, false);
        graphics.text(minecraft.font, subtitle, x + 10, y + 24, GhostHudStyle.TEXT, false);
    }
}
