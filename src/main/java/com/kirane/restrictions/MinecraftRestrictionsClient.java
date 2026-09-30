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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
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
                    previousGoal = goalForMask(oldStages);
                    goalTransitionUntil = System.currentTimeMillis() + 1100L;

                    if (context.client().player != null) {
                        context.client().player.playSound(
                                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                                1.0F,
                                1.0F
                        );
                    }
                }
            });
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                // Vanilla inventory panel is 176x166. Put the rules button directly to its right.
                int inventoryLeft = (scaledWidth - 176) / 2;
                int inventoryTop = (scaledHeight - 166) / 2;
                int buttonWidth = 80;
                int buttonX = inventoryLeft + 182;
                int buttonY = inventoryTop;

                Screens.getWidgets(screen).add(
                        Button.builder(Component.literal("Правила"), button -> {
                            client.gui.setScreen(new RestrictionsScreen(screen));
                        }).bounds(buttonX, buttonY, buttonWidth, 20).build()
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

        int width = 420;
        int height = 104;
        int x = (graphics.guiWidth() - width) / 2;
        int y = (graphics.guiHeight() - height) / 2;

        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xEC121212);
        graphics.fill(x, y, x + width, y + 4, 0xFFB0B0B0);
        graphics.fill(x, y + height - 4, x + width, y + height, 0xFF303030);
        graphics.fill(x, y, x + 4, y + height, 0xFFB0B0B0);
        graphics.fill(x + width - 4, y, x + width, y + height, 0xFF303030);

        ItemStack icon = notificationIcon(notificationStage);
        graphics.item(icon, x + 24, y + 44);

        var font = Minecraft.getInstance().font;
        String title = "ЗАДАНИЕ ВЫПОЛНЕНО";
        String subtitle = "НА ВАС НАЛОЖЕНО ОГРАНИЧЕНИЕ";

        int titleX = centeredX(font, title, x + 58, width - 82);
        int subtitleX = centeredX(font, subtitle, x + 58, width - 82);

        graphics.text(font, title, titleX, y + 26, 0xFFFFD83D, true);
        graphics.text(font, subtitle, subtitleX, y + 56, 0xFFFFFFFF, true);
    }

    private static ItemStack notificationIcon(int notificationStage) {
        int stage = Integer.numberOfTrailingZeros(notificationStage);

        return switch (stage) {
            case 0 -> new ItemStack(Items.IRON_INGOT);
            case 1 -> new ItemStack(Items.SHIELD);
            case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
            case 3 -> new ItemStack(Items.BOW);
            default -> new ItemStack(Items.BARRIER);
        };
    }

    private static int centeredX(net.minecraft.client.gui.Font font, String text, int areaX, int areaW) {
        return areaX + Math.max(0, (areaW - font.width(text)) / 2);
    }

    private static void renderGoal(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        String goal = goalForMask(activeStages);
        if (goal == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (goalTransitionUntil > now && previousGoal != null) {
            long remaining = goalTransitionUntil - now;
            long elapsed = 1100L - remaining;

            int boxAlpha = 255;
            if (elapsed > 700L) {
                boxAlpha = Math.max(0, 255 - (int) (((elapsed - 700L) / 400.0f) * 255.0f));
            }

            drawGoalBox(graphics, previousGoal, boxAlpha);

            // Visible strike-through animation: the red line grows across the entire box.
            float strikeProgress = Math.max(0.0f, Math.min(1.0f, elapsed / 700.0f));
            drawStrike(graphics, previousGoal, boxAlpha, strikeProgress);
            return;
        }

        drawGoalBox(graphics, goal, 255);
    }

    private static void drawGoalBox(GuiGraphicsExtractor graphics, String goal, int alpha) {
        var font = Minecraft.getInstance().font;

        int paddingX = 8;
        int height = 22;
        int width = font.width(goal) + paddingX * 2;
        int x = 8;
        int y = 8;

        int backgroundAlpha = Math.max(0, Math.min(255, (alpha * 0x55) / 255));
        int bg = (backgroundAlpha << 24) | 0x111111;
        int text = (alpha << 24) | 0xFFD83D;

        graphics.fill(x, y, x + width, y + height, bg);

        int textY = y + (height - font.lineHeight) / 2;
        graphics.text(font, goal, x + paddingX, textY, text, true);
    }

    private static void drawStrike(GuiGraphicsExtractor graphics, String goal, int alpha, float progress) {
        var font = Minecraft.getInstance().font;

        int x = 8;
        int y = 8;
        int textWidth = font.width(goal);

        int redAlpha = Math.max(0, Math.min(255, alpha));
        int red = (redAlpha << 24) | 0xE13B3B;

        int lineWidth = Math.max(0, Math.round(textWidth + 8) * 0 + Math.round((textWidth + 2 * 8) * progress));
        graphics.fill(x, y + 10, x + lineWidth, y + 12, red);
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
