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
    private static final KeyMapping.Category CONTROLS_CATEGORY =
            KeyMapping.Category.register(Identifier.parse("minecraft_restrictions:controls"));

    private static final KeyMapping RULES_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minecraft_restrictions.rules",
                    InputConstants.KEY_J,
                    CONTROLS_CATEGORY
            )
    );

    private static final KeyMapping GOAL_VISIBILITY_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.minecraft_restrictions.goal_visibility",
                    InputConstants.KEY_P,
                    CONTROLS_CATEGORY
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
                    goalTransitionUntil = System.currentTimeMillis() + 2600L;

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
                int buttonX = inventoryLeft - buttonWidth - 6;
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

        final int width = 390;
        final int height = 104;
        final int x = (graphics.guiWidth() - width) / 2;
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

        int titleX = x + (width - font.width(title)) / 2;
        int subtitleX = x + (width - font.width(subtitle)) / 2;

        // Only vertical placement is changed: the text is moved slightly down.
        graphics.text(font, title, titleX, y + 36, 0xFFFFD83D, true);
        graphics.text(font, subtitle, subtitleX, y + 66, 0xFFFFFFFF, true);

        final float scale = 1.75f;
        final int iconSize = Math.round(16 * scale);
        final int iconY = y + (height - iconSize) / 2;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 78, iconY);
        graphics.pose().scale(scale, scale);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        graphics.pose().translate(x + width - 78 - iconSize, iconY);
        graphics.pose().scale(scale, scale);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();
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
            long elapsed = 2600L - remaining;

            int boxAlpha = 255;
            if (elapsed > 1500L) {
                boxAlpha = Math.max(0, 255 - (int) (((elapsed - 1500L) / 1100.0f) * 255.0f));
            }

            drawGoalBox(graphics, previousGoal, boxAlpha);

            float strikeProgress = Math.max(0.0f, Math.min(1.0f, elapsed / 1500.0f));
            drawStrike(graphics, previousGoal, boxAlpha, strikeProgress);
            return;
        }

        drawGoalBox(graphics, goal, 255);
    }

    private static void drawGoalBox(GuiGraphicsExtractor graphics, String goal, int alpha) {
        var font = Minecraft.getInstance().font;

        final int paddingX = 8;
        final int width = font.width(goal) + paddingX * 2;
        final int height = 22;
        final int x = 8;
        final int y = 8;

        int backgroundAlpha = Math.max(0, Math.min(255, (alpha * 0x55) / 255));
        int bg = (backgroundAlpha << 24) | 0x111111;
        graphics.fill(x, y, x + width, y + height, bg);

        int textY = y + (height - font.lineHeight) / 2;
        graphics.text(font, goal, x + paddingX, textY, (alpha << 24) | 0xFFD83D, true);
    }

    private static void drawStrike(GuiGraphicsExtractor graphics, String goal, int alpha, float progress) {
        var font = Minecraft.getInstance().font;

        final int width = font.width(goal) + 16;
        final int x = 8;
        final int y = 8;

        int redAlpha = Math.max(0, Math.min(255, alpha));
        int red = (redAlpha << 24) | 0xE13B3B;

        int textWidth = font.width(goal);
        int textLeft = x + (width - textWidth) / 2;
        int strikeWidth = Math.round((textWidth + 16) * progress);
        int strikeX = textLeft - 8;

        graphics.fill(strikeX, y + 10, strikeX + strikeWidth, y + 12, red);
    }

    private static String goalForMask(int mask) {
        if ((mask & 1) == 0) return "Добыть железо";
        if ((mask & 2) == 0) return "Добыть алмазы";
        if ((mask & 4) == 0) return "Попасть в Незер";
        if ((mask & 8) == 0) return "Попасть в адскую крепость";
        if ((mask & 16) == 0) return "Попасть в Эндер край";
        if ((mask & 32) == 0) return "Убить ЭНДЕР-ДРАКОНА";
        return null;
    }
}
