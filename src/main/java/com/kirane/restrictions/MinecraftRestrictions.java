package com.kirane.restrictions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftRestrictions implements ModInitializer {
    public static final String MOD_ID = "minecraft_restrictions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String IRON_STAGE = "mr_iron";
    private static final String DIAMOND_STAGE = "mr_diamond";
    private static final String NETHER_STAGE = "mr_nether";
    private static final String FORTRESS_STAGE = "mr_fortress";
    private static final String END_STAGE = "mr_end";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(MinecraftRestrictions::tick);

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!player.getTags().contains(END_STAGE)) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BlockItem) {
                player.sendSystemMessage(Component.literal("§cБлоки запрещены."));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        LOGGER.info("Minecraft Restrictions loaded for Minecraft 26.3.");
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateProgression(player);
            enforceRestrictions(player);

            if (player.tickCount % 10 == 0) {
                player.sendOverlayMessage(Restrictions.hud(player));
            }
        }
    }

    private static void updateProgression(ServerPlayer player) {
        if (!player.getTags().contains(IRON_STAGE) && hasItem(player, Items.IRON_INGOT)) {
            player.addTag(IRON_STAGE);
            announce(player, "ЖЕЛЕЗО", "Спринт запрещён.");
        }

        if (player.getTags().contains(IRON_STAGE)
                && !player.getTags().contains(DIAMOND_STAGE)
                && hasItem(player, Items.DIAMOND)) {
            player.addTag(DIAMOND_STAGE);
            announce(player, "АЛМАЗЫ", "Щит запрещён.");
        }

        if (!player.getTags().contains(NETHER_STAGE)
                && player.level().dimension() == Level.NETHER) {
            player.addTag(NETHER_STAGE);
            announce(player, "НЕЗЕР", "Броня запрещена.");
            removeArmor(player);
        }

        if (player.getTags().contains(NETHER_STAGE)
                && !player.getTags().contains(FORTRESS_STAGE)
                && nearFortressBlocks(player)) {
            player.addTag(FORTRESS_STAGE);
            announce(player, "КРЕПОСТЬ", "Лук запрещён.");
        }

        if (!player.getTags().contains(END_STAGE)
                && player.level().dimension() == Level.END) {
            player.addTag(END_STAGE);
            announce(player, "ЭНД", "Ставить блоки запрещено.");
        }
    }

    private static void enforceRestrictions(ServerPlayer player) {
        if (player.getTags().contains(IRON_STAGE)) {
            player.setSprinting(false);
        }

        if (player.getTags().contains(DIAMOND_STAGE)) {
            if (player.isUsingItem() && player.getUseItem().is(Items.SHIELD)) {
                player.stopUsingItem();
            }
            player.getCooldowns().addCooldown(Items.SHIELD, 2);
        }

        if (player.getTags().contains(NETHER_STAGE)) {
            removeArmor(player);
        }

        if (player.getTags().contains(FORTRESS_STAGE)) {
            if (player.isUsingItem() && player.getUseItem().is(Items.BOW)) {
                player.stopUsingItem();
            }
            player.getCooldowns().addCooldown(Items.BOW, 2);
        }
    }

    private static boolean hasItem(ServerPlayer player, Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void removeArmor(ServerPlayer player) {
        EquipmentSlot[] armor = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        for (EquipmentSlot slot : armor) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                player.getInventory().add(stack);
            }
        }
    }

    private static boolean nearFortressBlocks(ServerPlayer player) {
        BlockPos center = player.blockPosition();

        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    var block = player.level().getBlockState(pos).getBlock();

                    if (block == Blocks.NETHER_BRICKS
                            || block == Blocks.NETHER_BRICK_FENCE
                            || block == Blocks.NETHER_BRICK_STAIRS
                            || block == Blocks.NETHER_BRICK_SLAB
                            || block == Blocks.NETHER_BRICK_WALL) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void announce(ServerPlayer player, String trigger, String restriction) {
        player.sendSystemMessage(Component.literal("§6§lОГРАНИЧЕНИЕ АКТИВИРОВАНО"));
        player.sendSystemMessage(Component.literal("§e" + trigger + " §7→ §c" + restriction));
        player.sendOverlayMessage(Component.literal("§c✕ " + restriction));
    }

    private static final class Restrictions {
        private static Component hud(ServerPlayer player) {
            StringBuilder text = new StringBuilder("§6ОГРАНИЧЕНИЯ §8| ");

            if (player.getTags().contains(IRON_STAGE)) text.append("§cСпринт");
            if (player.getTags().contains(DIAMOND_STAGE)) text.append(" §8• §cЩит");
            if (player.getTags().contains(NETHER_STAGE)) text.append(" §8• §cБроня");
            if (player.getTags().contains(FORTRESS_STAGE)) text.append(" §8• §cЛук");
            if (player.getTags().contains(END_STAGE)) text.append(" §8• §cБлоки");

            return Component.literal(text.toString());
        }
    }
}
