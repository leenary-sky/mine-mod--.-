package com.kirane.restrictions;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MinecraftRestrictionsClient implements ClientModInitializer {
    private static final KeyMapping RULES_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minecraft_restrictions.rules",
                    InputConstants.KEY_J,
                    KeyMapping.Category.register(Identifier.parse("minecraft_restrictions:controls"))
            )
    );

    private static int activeStages;
    private static int notificationStage;
    private static long notificationUntil;

    private static int previousStages;
    private static long goalTransitionUntil;
    private static String previousGoal;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RestrictionPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                int oldStages = activeStages;
                activeStages = payload.mask();
                MinecraftRestrictionsClientState.setMask(activeStages);
                notificationStage = payload.notificationStage();

                if (payload.notificationStage() != 0 && oldStages != activeStages) {
                    notificationUntil = System.currentTimeMillis() + 7000L;
                    previousStages = oldStages;
                    previousGoal = goalForMask(oldStages);
                    goalTransitionUntil = System.currentTimeMillis() + 900L;
                }
            });
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                // Small button above the inventory, away from slots and crafting controls.
                int buttonX = scaledWidth / 2 + 48;
                int buttonY = scaledHeight / 2 - 96;
                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("Правила"), button -> {
                            client.gui.setScreen(new RestrictionsScreen());
                        }).bounds(buttonX, buttonY, 70, 20).build()
                );
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (RULES_KEY.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(new RestrictionsScreen());
                }
            }

            if (client.player != null && (activeStages & 1) != 0) {
                client.player.setSprinting(false);
                client.options.keySprint.setDown(false);
            }
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.parse("minecraft_restrictions:notification"),
                MinecraftRestrictionsClient::renderNotification
        );

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.parse("minecraft_restrictions:goal"),
                MinecraftRestrictionsClient::renderGoal
        );
    }

    private static void renderNotification(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (notificationStage == 0 || System.currentTimeMillis() >= notificationUntil) {
            return;
        }

        int stage = Integer.numberOfTrailingZeros(notificationStage);
        String trigger;
        String restriction;
        ItemStack icon;

        switch (stage) {
            case 0 -> {
                trigger = "ЖЕЛЕЗО";
                restriction = "СПРИНТ ЗАПРЕЩЁН";
                icon = new ItemStack(Items.IRON_INGOT);
            }
            case 1 -> {
                trigger = "АЛМАЗЫ";
                restriction = "ЩИТ ЗАПРЕЩЁН";
                icon = new ItemStack(Items.SHIELD);
            }
            case 2 -> {
                trigger = "НЕЗЕР";
                restriction = "БРОНЯ ЗАПРЕЩЕНА";
                icon = new ItemStack(Items.IRON_CHESTPLATE);
            }
            case 3 -> {
                trigger = "АДСКАЯ КРЕПОСТЬ";
                restriction = "ЛУК ЗАПРЕЩЁН";
                icon = new ItemStack(Items.BOW);
            }
            default -> {
                trigger = "ЭНДЕР КРАЙ";
                restriction = "БЛОКИ ЗАПРЕЩЕНЫ";
                icon = new ItemStack(Items.BARRIER);
            }
        }

        int width = 330;
        int height = 90;
        int x = (graphics.guiWidth() - width) / 2;
        int y = (graphics.guiHeight() - height) / 2;

        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xE5101010);
        graphics.fill(x, y, x + width, y + 4, 0xFF8A8A8A);
        graphics.fill(x, y + height - 4, x + width, y + height, 0xFF202020);
        graphics.fill(x, y, x + 4, y + height, 0xFF8A8A8A);
        graphics.fill(x + width - 4, y, x + width, y + height, 0xFF202020);

        graphics.item(icon, x + 20, y + 25);
        graphics.text(Minecraft.getInstance().font, "ОГРАНИЧЕНИЕ АКТИВИРОВАНО",
                x + 58, y + 18, 0xFFFFD83D, true);
        graphics.text(Minecraft.getInstance().font, trigger + "  →  " + restriction,
                x + 58, y + 44, 0xFFFFFFFF, true);
        graphics.text(Minecraft.getInstance().font, "J — открыть правила",
                x + 58, y + 65, 0xFFAAAAAA, false);
    }

    private static void renderGoal(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        String goal = goalForMask(activeStages);
        if (goal == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (goalTransitionUntil > now && previousGoal != null) {
            float progress = (goalTransitionUntil - now) / 900.0f;
            int alpha = Math.max(0, Math.min(255, (int) (progress * 255)));
            drawGoalBox(graphics, previousGoal, alpha);
            drawStrike(graphics, previousGoal, alpha);
            return;
        }

        drawGoalBox(graphics, goal, 255);
    }

    private static void drawGoalBox(GuiGraphicsExtractor graphics, String goal, int alpha) {
        var font = Minecraft.getInstance().font;

        int paddingX = 8;
        int paddingY = 5;
        int width = font.width(goal) + paddingX * 2;
        int height = 22;
        int x = 8;
        int y = 8;

        int bg = (alpha << 24) | 0x555555;
        int text = (alpha << 24) | 0xFFFFFF;

        graphics.fill(x, y, x + width, y + height, bg);
        graphics.text(font, goal, x + paddingX, y + paddingY, text, false);
    }

    private static void drawStrike(GuiGraphicsExtractor graphics, String goal, int alpha) {
        var font = Minecraft.getInstance().font;

        int x = 8;
        int y = 8;
        int textWidth = font.width(goal);
        int strikeY = y + 11;

        int red = (alpha << 24) | 0xD12C2C;
        graphics.fill(x + 7, strikeY, x + 7 + textWidth, strikeY + 2, red);
    }

    private static String goalForMask(int mask) {
        if ((mask & 1) == 0) return "Добыть железо";
        if ((mask & 2) == 0) return "Добыть алмазы";
        if ((mask & 4) == 0) return "Попасть в Незер";
        if ((mask & 8) == 0) return "Попасть в адскую крепость";
        if ((mask & 16) == 0) return "Попасть в Эндер край";
        return "Убить ЭНДЕР-ДРАКОНА";
    }
}
