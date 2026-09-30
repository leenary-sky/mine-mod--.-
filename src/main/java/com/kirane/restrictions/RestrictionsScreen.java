package com.kirane.restrictions;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RestrictionsScreen extends Screen {
    private static final int PANEL_W = 430;
    private static final int PANEL_H = 300;

    public RestrictionsScreen() {
        super(Component.literal("Правила испытания"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;

        graphics.fill(0, 0, width, height, 0x99000000);

        // Pixel-style frame inspired by vanilla Minecraft GUI.
        graphics.fill(left + 4, top + 4, left + PANEL_W - 4, top + PANEL_H - 4, 0xFF1A1A1A);
        graphics.fill(left, top, left + PANEL_W, top + 4, 0xFF8A8A8A);
        graphics.fill(left, top + PANEL_H - 4, left + PANEL_W, top + PANEL_H, 0xFF202020);
        graphics.fill(left, top, left + 4, top + PANEL_H, 0xFF8A8A8A);
        graphics.fill(left + PANEL_W - 4, top, left + PANEL_W, top + PANEL_H, 0xFF202020);

        graphics.text(font, "ПРАВИЛА ИСПЫТАНИЯ", left + 20, top + 16, 0xFFFFFFFF, true);
        graphics.text(font, "Каждое усиление забирает одну возможность.", left + 20, top + 34, 0xFFAAAAAA, false);

        drawRule(graphics, left + 20, top + 62, new ItemStack(Items.IRON_INGOT),
                "ЖЕЛЕЗО", "СПРИНТ ЗАПРЕЩЁН");
        drawRule(graphics, left + 20, top + 103, new ItemStack(Items.DIAMOND),
                "АЛМАЗЫ", "ЩИТ ЗАПРЕЩЁН");
        drawRule(graphics, left + 20, top + 144, new ItemStack(Items.NETHERRACK),
                "Попасть в Незер", "БРОНЯ ЗАПРЕЩЕНА");
        drawRule(graphics, left + 20, top + 185, new ItemStack(Items.NETHER_BRICKS),
                "АДСКАЯ КРЕПОСТЬ", "ЛУК ЗАПРЕЩЁН");
        drawRule(graphics, left + 20, top + 226, new ItemStack(Items.END_STONE),
                "КРАЙ", "БЛОКИ ЗАПРЕЩЕНЫ");

        graphics.text(font, "ЦЕЛЬ: УБИТЬ ЭНДЕР-ДРАКОНА", left + 20, top + 270, 0xFFFFD83D, true);
    }

    private void drawRule(GuiGraphicsExtractor graphics, int x, int y, ItemStack icon,
                          String trigger, String restriction) {
        graphics.fill(x, y - 3, x + PANEL_W - 40, y + 31, 0xFF242424);
        graphics.item(icon, x + 7, y + 1);
        graphics.text(font, trigger, x + 34, y + 3, 0xFFFFFFFF, true);
        graphics.text(font, "→", x + 170, y + 3, 0xFFB0B0B0, true);
        graphics.text(font, restriction, x + 198, y + 3, 0xFFFF6B6B, true);
    }
}
