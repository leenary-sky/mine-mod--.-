package com.kirane.restrictions;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
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
    protected void init() {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        addRenderableWidget(
                Button.builder(Component.literal("←"), button -> goBack())
                        .bounds(left + 10, top + 10, 24, 20)
                        .build()
        );
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        int mask = MinecraftRestrictionsClientState.mask();

        graphics.fill(0, 0, width, height, 0x99000000);

        graphics.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xFF1A1A1A);
        graphics.fill(left, top, left + PANEL_W, top + 4, 0xFF8A8A8A);
        graphics.fill(left, top + PANEL_H - 4, left + PANEL_W, top + PANEL_H, 0xFF202020);
        graphics.fill(left, top, left + 4, top + PANEL_H, 0xFF8A8A8A);
        graphics.fill(left + PANEL_W - 4, top, left + PANEL_W, top + PANEL_H, 0xFF202020);

        graphics.text(font, "ПРАВИЛА ИСПЫТАНИЯ", left + 44, top + 16, 0xFFFFFFFF, true);
        graphics.text(font, "Каждое усиление забирает одну возможность.",
                left + 20, top + 40, 0xFFAAAAAA, false);

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
        graphics.fill(x, y - 3, x + PANEL_W - 40, y + 31, 0xFF242424);
        graphics.item(icon, x + 7, y + 1);

        int triggerX = x + 34;
        graphics.text(font, trigger, triggerX, y + 3, 0xFFFFFFFF, true);
        graphics.text(font, "→", x + 188, y + 3, 0xFFB0B0B0, true);
        graphics.text(font, restriction, x + 216, y + 3, 0xFFFF6B6B, true);

        if (completed) {
            int strikeStart = triggerX;
            int strikeEnd = triggerX + font.width(trigger);
            graphics.fill(strikeStart, y + 12, strikeEnd, y + 14, 0xFFE13B3B);
        }
    }
}
