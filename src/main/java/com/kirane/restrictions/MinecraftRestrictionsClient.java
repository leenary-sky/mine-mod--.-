package com.kirane.restrictions;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
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

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RestrictionPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                activeStages = payload.mask();
                notificationStage = payload.notificationStage();
                notificationUntil = System.currentTimeMillis() + 3500L;
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (RULES_KEY.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(new RestrictionsScreen());
                }
            }

            // Stop both Ctrl-style sprinting and double-W sprinting on the client.
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
                trigger = "НИЗШИЙ МИР";
                restriction = "БРОНЯ ЗАПРЕЩЕНА";
                icon = new ItemStack(Items.IRON_CHESTPLATE);
            }
            case 3 -> {
                trigger = "АДСКАЯ КРЕПОСТЬ";
                restriction = "ЛУК ЗАПРЕЩЁН";
                icon = new ItemStack(Items.BOW);
            }
            default -> {
                trigger = "КРАЙ";
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
}
