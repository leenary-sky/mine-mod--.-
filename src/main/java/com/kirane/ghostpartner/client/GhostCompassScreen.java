package com.kirane.ghostpartner.client;

import com.kirane.ghostpartner.RoundHudPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A small, recording-friendly rescue panel opened by right-clicking the rescue compass. */
public final class GhostCompassScreen extends Screen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 188;
    private static final int PANEL = 0xE9101118;
    private static final int ACCENT = 0xFF79E7E0;
    private static final int MUTED = 0xFFBAC4D0;

    public GhostCompassScreen() {
        super(Component.literal("Компас спасения"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraft = Minecraft.getInstance();
        RoundHudPayload state = GhostPartnerClient.roundState();
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(240, width - 24));
        int left = (width - panelWidth) / 2;
        int top = (height - PANEL_HEIGHT) / 2;

        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left + 3, top + 3, left + panelWidth - 3, top + PANEL_HEIGHT - 3, PANEL);
        graphics.fill(left, top, left + panelWidth, top + 3, ACCENT);
        graphics.fill(left, top + PANEL_HEIGHT - 3, left + panelWidth, top + PANEL_HEIGHT, 0xFF27323A);
        graphics.fill(left, top, left + 3, top + PANEL_HEIGHT, ACCENT);
        graphics.fill(left + panelWidth - 3, top, left + panelWidth, top + PANEL_HEIGHT, 0xFF27323A);

        graphics.item(new ItemStack(Items.COMPASS), left + 18, top + 18);
        graphics.text(font, "КОМПАС СПАСЕНИЯ", left + 42, top + 20, ACCENT, true);
        graphics.text(font, "Навигация к месту гибели напарника", left + 18, top + 46, MUTED, false);

        if (!state.active() || state.role() == 0) {
            graphics.text(font, "Сейчас раунд спасения не запущен.", left + 18, top + 76, 0xFFFFFFFF, false);
            graphics.text(font, "Компас снова заработает после смерти напарника.", left + 18, top + 96, MUTED, false);
        } else {
            int seconds = Math.max(0, state.remainingSeconds());
            String timer = String.format("%02d:%02d", seconds / 60, seconds % 60);
            graphics.text(font, "ОСТАЛОСЬ ВРЕМЕНИ", left + 18, top + 72, MUTED, false);
            graphics.text(font, timer, left + panelWidth - 66, top + 70,
                    seconds <= 30 ? 0xFFFF7777 : 0xFFFFFFFF, true);

            if (!state.targetInSameDimension()) {
                graphics.text(font, "МАЯК В ДРУГОМ ИЗМЕРЕНИИ", left + 18, top + 96, 0xFFFFD27A, true);
                graphics.text(font, "Вернись в измерение, где погиб напарник.", left + 18, top + 116, MUTED, false);
            } else if (minecraft.player != null) {
                BlockPos target = state.target();
                double distance = minecraft.player.position().distanceTo(Vec3.atCenterOf(target));
                String direction = directionArrow(minecraft, target);
                graphics.text(font, direction + "  " + Math.round(distance) + " блоков до точки смерти",
                        left + 18, top + 96, 0xFFFFFFFF, true);
                graphics.text(font, "Координаты: " + target.getX() + ", " + target.getY() + ", " + target.getZ(),
                        left + 18, top + 116, MUTED, false);
            }

            if (state.role() == 1) {
                int total = Math.max(1, state.ritualTotalTicks());
                int progress = Mth.clamp(state.ritualTicks(), 0, total);
                String ritual = progress > 0
                        ? "Ритуал: " + Math.round(progress * 100.0 / total) + "%"
                        : "На точке смерти удерживай Shift " + (total / 20) + " сек.";
                graphics.text(font, ritual, left + 18, top + 142, ACCENT, false);
            } else if (state.role() == 2) {
                graphics.text(font, "Ты призрак: веди напарника к точке смерти.",
                        left + 18, top + 142, ACCENT, false);
            }
        }

        String close = "Esc — закрыть";
        graphics.text(font, close, left + panelWidth - font.width(close) - 14,
                top + PANEL_HEIGHT - 23, MUTED, false);
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
}
