package com.kirane.ghostpartner.client;

import com.kirane.ghostpartner.GhostPartnerMod;
import com.kirane.ghostpartner.RoundHudPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Temporary, deliberately simple HUD. The layout can be replaced without changing round logic.
 * Art direction and asset files are intentionally kept separate from this renderer.
 */
public final class GhostPartnerClient implements ClientModInitializer {
    private static volatile RoundHudPayload state =
            new RoundHudPayload(false, (byte) 0, 0, BlockPos.ZERO, 0, 100);

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RoundHudPayload.TYPE,
                (payload, context) -> context.client().execute(() -> state = payload));
        HudElementRegistry.addLast(
                Identifier.parse(GhostPartnerMod.MOD_ID + ":round_hud"),
                GhostPartnerClient::renderHud
        );
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

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
        double distance = minecraft.player.position().distanceTo(Vec3.atCenterOf(target));
        String direction = directionArrow(minecraft, target);
        graphics.text(minecraft.font, direction + "  МАЯК  ·  " + Math.round(distance) + " м",
                x + 10, y + 25, GhostHudStyle.TEXT, false);

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
