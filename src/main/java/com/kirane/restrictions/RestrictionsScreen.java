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

        graphics.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xFF121212);
        graphics.fill(left, top, left + PANEL_W, top + 4, 0xFFB0B0B0);
        graphics.fill(left, top + PANEL_H - 4, left + PANEL_W, top + PANEL_H, 0xFF303030);
        graphics.fill(left, top, left + 4, top + PANEL_H, 0xFFB0B0B0);
        graphics.fill(left + PANEL_W - 4, top, left + PANEL_W, top + PANEL_H, 0xFF303030);

        graphics.text(font, "←", left + 11, top + 11, 0xFFFFFFFF, true);

        graphics.text(font, "ПРАВИЛА ИСПЫТАНИЯ",
                left + 44, top + 16, 0xFFFFFFFF, true);
        graphics.text(font, "Каждое усиление забирает одну возможность.",
                left + 20, top + 40, 0xFF808080, false);

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
                    left + 20, top + 270, 0xFFFFD83D, true);
        }
    }

    private void drawRule(GuiGraphicsExtractor graphics, int x, int y, ItemStack icon,
                          String trigger, String restriction, boolean completed) {
        final int rowW = PANEL_W - 40;
        final int rowH = 34;

        graphics.fill(x, y - 3, x + rowW, y + rowH - 3, 0xAA1D1D1D);
        graphics.item(icon, x + 6, y + 6);

        int textY = y + (rowH - font.lineHeight) / 2 - 3;

        int triggerAreaX = x + 28;
        int triggerAreaW = 180;
        int arrowX = x + 210;
        int restrictionAreaX = x + 224;
        int restrictionAreaW = rowW - 224;

        int triggerTextX = centeredX(trigger, triggerAreaX, triggerAreaW);
        int restrictionTextX = centeredX(restriction, restrictionAreaX, restrictionAreaW);

        graphics.text(font, trigger, triggerTextX, textY, 0xFFFFFFFF, true);
        graphics.text(font, "→", arrowX, textY, 0xFFB0B0B0, true);
        graphics.text(font, restriction, restrictionTextX, textY, 0xFFFF6B6B, true);

        if (completed) {
            int strikeY = textY + font.lineHeight / 2;
            graphics.fill(triggerTextX - 1, strikeY, triggerTextX + font.width(trigger) + 1, strikeY + 2,
                    0xFFE13B3B);
        }
    }

    private int centeredX(String text, int areaX, int areaW) {
        return areaX + Math.max(0, (areaW - font.width(text)) / 2);
    }
}
