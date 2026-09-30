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

    private static final KeyMapping GOAL_VISIBILITY_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minecraft_restrictions.goal_visibility",
                    InputConstants.KEY_P,
                    KeyMapping.Category.register(Identifier.parse("minecraft_restrictions:controls"))
            )
    );

    private static int activeStages;
    private static int notificationStage;
    private static long notificationUntil;

    private static long goalTransitionUntil;
    private static String previousGoal;
    private static boolean goalVisible = true;

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
                    goalTransitionUntil = System.currentTimeMillis() + 1800L;

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

            while (GOAL_VISIBILITY_KEY.consumeClick()) {
                goalVisible = !goalVisible;
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

        final int width = 420;
        final int height = 104;
        final int x = (graphics.guiWidth() - width) / 2;
        // Keep the restriction card clearly above the crosshair/center HUD.
        final int y = (graphics.guiHeight() - height) / 2 - 58;

        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, 0xE7121212);
        graphics.fill(x, y, x + width, y + 4, 0xFFB0B0B0);
        graphics.fill(x, y + height - 4, x + width, y + height, 0xFF303030);
        graphics.fill(x, y, x + 4, y + height, 0xFFB0B0B0);
        graphics.fill(x + width - 4, y, x + width, y + height, 0xFF303030);

        ItemStack icon = notificationIcon(notificationStage);
        var font = Minecraft.getInstance().font;
        String title = "ЗАДАНИЕ ВЫПОЛНЕНО";
        String subtitle = "НА ВАС НАЛОЖЕНО ОГРАНИЧЕНИЕ";

        int textWidth = Math.max(font.width(title), font.width(subtitle));
        int iconAreaWidth = 28;
        int gap = 16;
        int contentWidth = iconAreaWidth + gap + textWidth;
        int contentStartX = x + (width - contentWidth) / 2;

        // Scale the item to 175% and keep it close to the text.
        graphics.pose().pushMatrix();
        graphics.pose().translate(contentStartX, y + 36);
        graphics.pose().scale(1.75f, 1.75f);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();

        int textX = contentStartX + iconAreaWidth + gap;
        graphics.text(font, title, centeredX(font, title, textX, textWidth), y + 26, 0xFFFFD83D, true);
        graphics.text(font, subtitle, centeredX(font, subtitle, textX, textWidth), y + 56, 0xFFFFFFFF, true);
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
        if (!goalVisible) {
            return;
        }

        String goal = goalForMask(activeStages);
        if (goal == null) {
            return;
        }

        long now = System.currentTimeMillis();

        if (goalTransitionUntil > now && previousGoal != null) {
            long remaining = goalTransitionUntil - now;
            long elapsed = 1800L - remaining;

            int boxAlpha = 255;
            if (elapsed > 1150L) {
                boxAlpha = Math.max(0, 255 - (int) (((elapsed - 1150L) / 650.0f) * 255.0f));
            }

            drawGoalBox(graphics, previousGoal, boxAlpha);

            // Slow, highly visible strike-through animation.
            float strikeProgress = Math.max(0.0f, Math.min(1.0f, elapsed / 1150.0f));
            drawStrike(graphics, previousGoal, boxAlpha, strikeProgress);
            return;
        }

        drawGoalBox(graphics, goal, 255);
    }

    private static void drawGoalBox(GuiGraphicsExtractor graphics, String goal, int alpha) {
        var font = Minecraft.getInstance().font;

        final int width = Math.max(220, font.width(goal) + 34);
        final int height = 30;
        final int x = 8;
        final int y = 8;

        int backgroundAlpha = Math.max(0, Math.min(255, (alpha * 0x55) / 255));
        int bg = (backgroundAlpha << 24) | 0x111111;
        graphics.fill(x, y, x + width, y + height, bg);

        int baseTextWidth = font.width(goal);
        int centeredTextX = x + (width - baseTextWidth) / 2;
        int textY = y + (height - font.lineHeight) / 2;

        // Slightly larger task text while keeping it exactly centered in the box.
        graphics.pose().pushMatrix();
        float scale = 1.25f;
        float centerX = x + width / 2.0f;
        float centerY = y + height / 2.0f;
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-centerX, -centerY);
        graphics.text(font, goal, centeredTextX, textY, (alpha << 24) | 0xFFD83D, true);
        graphics.pose().popMatrix();
    }

    private static void drawStrike(GuiGraphicsExtractor graphics, String goal, int alpha, float progress) {
        var font = Minecraft.getInstance().font;

        final int width = Math.max(220, font.width(goal) + 34);
        final int height = 30;
        final int x = 8;
        final int y = 8;

        int redAlpha = Math.max(0, Math.min(255, alpha));
        int red = (redAlpha << 24) | 0xE13B3B;

        int textWidth = font.width(goal);
        int textLeft = x + (width - textWidth) / 2;

        // Slightly thicker and longer than before so the completion is unmistakable.
        int strikeWidth = Math.round((textWidth + 16) * progress);
        int strikeX = textLeft - 8;

        graphics.fill(strikeX, y + height / 2 - 1, strikeX + strikeWidth,
                y + height / 2 + 2, red);
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
