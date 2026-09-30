package com.kirane.restrictions;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RestrictionsScreen extends Screen {
    private static final int PANEL_W = 430;
    private static final int PANEL_H = 300;

    private final Screen parent;

    public RestrictionsScreen() {
        this(null);
    }

    public RestrictionsScreen(Screen parent) {
        super(Component.literal("Правила испытания"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        goBack();
    }

    private void goBack() {
        if (minecraft == null) {
            return;
        }

        if (parent != null) {
            minecraft.gui.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        if (event.button() == 0
                && event.x() >= left + 8 && event.x() <= left + 34
                && event.y() >= top + 8 && event.y() <= top + 34) {
            goBack();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        int mask = MinecraftRestrictionsClientState.mask();

        graphics.fill(0, 0, width, height, 0x99000000);

        // Extremely dark interior; the border is intentionally lighter.
        graphics.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xFF121212);
        graphics.fill(left, top, left + PANEL_W, top + 4, 0xFFB0B0B0);
        graphics.fill(left, top + PANEL_H - 4, left + PANEL_W, top + PANEL_H, 0xFF303030);
        graphics.fill(left, top, left + 4, top + PANEL_H, 0xFFB0B0B0);
        graphics.fill(left + PANEL_W - 4, top, left + PANEL_W, top + PANEL_H, 0xFF303030);

        // White arrow with shadow, matching the title treatment.
        graphics.text(font, "←", left + 11, top + 11, 0xFFFFFFFF, true);

        String title = "ПРАВИЛА ИСПЫТАНИЯ";
        String subtitle = "Каждое усиление забирает одну возможность.";

        graphics.text(font, title,
                centeredX(title, left, PANEL_W), top + 16, 0xFFFFFFFF, true);
        graphics.text(font, subtitle,
                centeredX(subtitle, left, PANEL_W), top + 40, 0xFF808080, false);

        drawRule(graphics, left + 20, top + 68, new ItemStack(Items.IRON_INGOT),
                "Добыть железо", "Спринт запрещён", (mask & 1) != 0);
        drawRule(graphics, left + 20, top + 108, new ItemStack(Items.DIAMOND),
                "Добыть алмазы", "Щит запрещён", (mask & 2) != 0);
        drawRule(graphics, left + 20, top + 148, new ItemStack(Items.NETHERRACK),
                "Попасть в Незер", "Броня запрещена", (mask & 4) != 0);
        drawRule(graphics, left + 20, top + 188, new ItemStack(Items.NETHER_BRICKS),
                "Попасть в адскую крепость", "Лук запрещён", (mask & 8) != 0);
        drawRule(graphics, left + 20, top + 228, new ItemStack(Items.END_STONE),
                "Попасть в Эндер край", "Блоки запрещены", (mask & 16) != 0);

        if ((mask & 16) != 0) {
            graphics.text(font, "ЦЕЛЬ: УБИТЬ ЭНДЕР-ДРАКОНА",
                    centeredX("ЦЕЛЬ: УБИТЬ ЭНДЕР-ДРАКОНА", left, PANEL_W),
                    top + 270, 0xFFFFD83D, true);
        }
    }

    private void drawRule(GuiGraphicsExtractor graphics, int x, int y, ItemStack icon,
                          String trigger, String restriction, boolean completed) {
        final int rowW = PANEL_W - 40;
        final int rowH = 34;
        final int rowTop = y - 3;

        // Subtle translucent row, not a bright solid plate.
        graphics.fill(x, rowTop, x + rowW, rowTop + rowH, 0x8A1D1D1D);

        final int iconAreaW = 36;
        final int triggerAreaW = 174;
        final int arrowAreaW = 28;
        final int restrictionAreaW = rowW - iconAreaW - triggerAreaW - arrowAreaW;

        int iconX = x + (iconAreaW - 16) / 2;
        int iconY = rowTop + (rowH - 16) / 2;
        graphics.item(icon, iconX, iconY);

        int triggerX = x + iconAreaW;
        int arrowX = triggerX + triggerAreaW;
        int restrictionX = arrowX + arrowAreaW;

        int textY = rowTop + (rowH - font.lineHeight) / 2;

        graphics.text(font, trigger,
                centeredX(trigger, triggerX, triggerAreaW), textY, 0xFFFFFFFF, true);

        graphics.text(font, "→",
                centeredX("→", arrowX, arrowAreaW), textY, 0xFFB0B0B0, true);

        graphics.text(font, restriction,
                centeredX(restriction, restrictionX, restrictionAreaW),
                textY, 0xFFFF6B6B, true);

        // Completed rule: strike through the whole rectangular rule block.
        if (completed) {
            int strikeY = rowTop + rowH / 2 - 1;
            graphics.fill(x + 2, strikeY, x + rowW - 2, strikeY + 2, 0xFFE13B3B);
        }
    }

    private int centeredX(String text, int areaX, int areaW) {
        return areaX + Math.max(0, (areaW - font.width(text)) / 2);
    }
}
