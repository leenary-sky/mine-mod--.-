package com.kirane.ghostpartner.client;

import com.kirane.ghostpartner.GhostOrbitDistancePayload;
import com.kirane.ghostpartner.GhostPartnerMod;
import com.kirane.ghostpartner.RoundHudPayload;
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
    /** Implemented by GhostCameraMixin; nested here to keep it outside the mixin-owned package. */
    public interface CameraAccess {
        void ghostPartner$setPosition(Vec3 position);
        void ghostPartner$setRotation(float yaw, float pitch);
    }
    private static final double MIN_ORBIT_DISTANCE = 2.0;
    private static final int DEFAULT_ORBIT_DISTANCE_TENTHS = 50;
    private static volatile RoundHudPayload state =
            new RoundHudPayload(false, (byte) 0, 0, BlockPos.ZERO, false, 0, 100,
                    RoundHudPayload.packOrbitSettings(DEFAULT_ORBIT_DISTANCE_TENTHS, 100));
    private static volatile long transitionStartedAt = 0L;
    private static net.minecraft.client.player.LocalPlayer lastObservedPlayer;
    private static boolean respawnRequested;
    // A short blackout is enough to hide the respawn replacement without leaving the player
    // staring at a death screen for several seconds.
    private static final long FADE_IN_MS = 420L;
    private static final long BLACK_HOLD_MS = 120L;
    private static final long FADE_OUT_MS = 500L;
    private static final long TRANSITION_DURATION_MS = FADE_IN_MS + BLACK_HOLD_MS + FADE_OUT_MS;
    private static double orbitDistance = DEFAULT_ORBIT_DISTANCE_TENTHS / 10.0;
    private static double smoothX = Double.NaN, smoothY = Double.NaN, smoothZ = Double.NaN;
    private static long lastCameraUpdateNanos;

    public static boolean isGhostCameraActive() {
        return state.active() && state.role() == 2;
    }

    public static RoundHudPayload roundState() {
        return state;
    }

    public static boolean shouldOpenRescueCompass() {
        return state.active() && (state.role() == 1 || state.role() == 2);
    }

    public static void adjustOrbitZoom(double scroll) {
        if (!isGhostCameraActive()) return;
        double maxDistance = Math.max(MIN_ORBIT_DISTANCE, state.maxOrbitDistanceTenths() / 10.0);
        double nextDistance = Mth.clamp(orbitDistance - scroll * 0.8, MIN_ORBIT_DISTANCE, maxDistance);
        if (Math.abs(nextDistance - orbitDistance) < 0.05) return;

        orbitDistance = nextDistance;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            int distanceTenths = Mth.clamp((int) Math.round(nextDistance * 10.0),
                    (int) (MIN_ORBIT_DISTANCE * 10.0), state.maxOrbitDistanceTenths());
            ClientPlayNetworking.send(new GhostOrbitDistancePayload(distanceTenths));
        }
    }

    public static void applyOrbitCamera(net.minecraft.client.Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (!isGhostCameraActive() || mc.player == null || mc.level == null) {
            resetOrbitSmoothing();
            return;
        }
        net.minecraft.client.player.LocalPlayer ghost = mc.player;
        net.minecraft.client.player.AbstractClientPlayer target = null;
        for (net.minecraft.client.player.AbstractClientPlayer candidate : mc.level.players()) {
            if (candidate != ghost && candidate.isAlive() && !candidate.isSpectator()) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            resetOrbitSmoothing();
            return;
        }

        double targetX = target.getX();
        double targetY = target.getY() + target.getBbHeight() * 0.82;
        double targetZ = target.getZ();
        double yaw = Math.toRadians(ghost.getYRot());
        double pitch = Math.toRadians(Mth.clamp(ghost.getXRot(), -75.0f, 75.0f));
        double horizontal = Math.cos(pitch) * orbitDistance;
        double desiredX = targetX - Math.sin(yaw) * horizontal;
        double desiredY = targetY + Math.sin(pitch) * orbitDistance;
        double desiredZ = targetZ + Math.cos(yaw) * horizontal;

        // Keep the real local player entity under the detached camera. That makes the
        // client request chunks around the orbit instead of only around the old respawn point.
        // The body's height follows the survivor's jump; looking up/down moves only the camera.
        double bodyX = target.getX() - Math.sin(yaw) * orbitDistance;
        double bodyY = target.getY();
        double bodyZ = target.getZ() + Math.cos(yaw) * orbitDistance;
        ghost.setPos(bodyX, bodyY, bodyZ);

        long now = System.nanoTime();
        double frameSeconds = lastCameraUpdateNanos == 0L
                ? 1.0 / 60.0
                : Math.min(0.1, Math.max(0.0, (now - lastCameraUpdateNanos) / 1_000_000_000.0));
        lastCameraUpdateNanos = now;
        double smoothing = 1.0 - Math.exp(-frameSeconds * 13.0);
        if (Double.isNaN(smoothX)) {
            smoothX = desiredX;
            smoothY = desiredY;
            smoothZ = desiredZ;
        } else {
            smoothX += (desiredX - smoothX) * smoothing;
            smoothY += (desiredY - smoothY) * smoothing;
            smoothZ += (desiredZ - smoothZ) * smoothing;
        }
        ((CameraAccess) (Object) camera).ghostPartner$setPosition(new Vec3(smoothX, smoothY, smoothZ));
        double dx = targetX - smoothX, dy = targetY - smoothY, dz = targetZ - smoothZ;
        float lookYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float lookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        ((CameraAccess) (Object) camera).ghostPartner$setRotation(lookYaw, lookPitch);
    }

    private static void resetOrbitSmoothing() {
        smoothX = smoothY = smoothZ = Double.NaN;
        lastCameraUpdateNanos = 0L;
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RoundHudPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    state = payload;
                    if (payload.active() && payload.role() == 2) {
                        double maxDistance = Math.max(MIN_ORBIT_DISTANCE,
                                payload.maxOrbitDistanceTenths() / 10.0);
                        orbitDistance = Mth.clamp(payload.orbitDistanceTenths() / 10.0,
                                MIN_ORBIT_DISTANCE, maxDistance);
                    } else if (!payload.active() || payload.role() != 2) {
                        resetOrbitSmoothing();
                        if (payload.role() != 2) {
                            transitionStartedAt = 0L;
                            respawnRequested = false;
                        }
                    }
                }));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && state.active() && state.role() == 2
                    && client.player.getHealth() <= 0.0f) {
                if (transitionStartedAt == 0L) transitionStartedAt = System.currentTimeMillis();
                if (!respawnRequested && System.currentTimeMillis() - transitionStartedAt >= FADE_IN_MS + 50L
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
            String direction = directionArrow(target);
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

    public static String directionArrow(BlockPos target) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return "•";
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
