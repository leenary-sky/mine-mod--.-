package com.kirane.restrictions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.Identifier;
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
    private static final Identifier STORAGE_ID = Identifier.parse(MOD_ID + ":player_restrictions");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.clientboundPlay().register(RestrictionPayload.TYPE, RestrictionPayload.CODEC);

        ServerTickEvents.END_SERVER_TICK.register(MinecraftRestrictions::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerPlayNetworking.send(player, new RestrictionPayload(stageMask(player)));
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);

            if (hasStage(serverPlayer, DIAMOND_STAGE) && stack.is(Items.SHIELD)) {
                return InteractionResult.FAIL;
            }

            if (hasStage(serverPlayer, FORTRESS_STAGE) && stack.is(Items.BOW)) {
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
        }
    }

    private static void updateProgression(ServerPlayer player) {
        if (!hasStage(player, IRON_STAGE) && hasItem(player, Items.IRON_INGOT)) {
            activate(player, IRON_STAGE, 0);
        }

        if (hasStage(player, IRON_STAGE)
                && !hasStage(player, DIAMOND_STAGE)
                && hasItem(player, Items.DIAMOND)) {
            activate(player, DIAMOND_STAGE, 1);
        }

        if (!hasStage(player, NETHER_STAGE)
                && player.level().dimension() == Level.NETHER) {
            activate(player, NETHER_STAGE, 2);
            removeArmor(player);
        }

        if (hasStage(player, NETHER_STAGE)
                && !hasStage(player, FORTRESS_STAGE)
                && nearFortressBlocks(player)) {
            activate(player, FORTRESS_STAGE, 3);
        }

        if (!hasStage(player, END_STAGE)
                && player.level().dimension() == Level.END) {
            activate(player, END_STAGE, 4);
        }
    }

    private static void enforceRestrictions(ServerPlayer player) {
        if (hasStage(player, IRON_STAGE)) {
            player.setSprinting(false);
        }

        if (hasStage(player, NETHER_STAGE)) {
            removeArmor(player);
        }
    }

    private static void activate(ServerPlayer player, String stage, int notificationStage) {
        setStage(player, stage);
        ServerPlayNetworking.send(player, new RestrictionPayload(1 << notificationStage));
    }

    private static int stageMask(ServerPlayer player) {
        int mask = 0;
        if (hasStage(player, IRON_STAGE)) mask |= 1;
        if (hasStage(player, DIAMOND_STAGE)) mask |= 2;
        if (hasStage(player, NETHER_STAGE)) mask |= 4;
        if (hasStage(player, FORTRESS_STAGE)) mask |= 8;
        if (hasStage(player, END_STAGE)) mask |= 16;
        return mask;
    }

    public static boolean hasEndRestriction(ServerPlayer player) {
        return hasStage(player, END_STAGE);
    }

    private static boolean hasStage(ServerPlayer player, String stage) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        CompoundTag data = server.getCommandStorage().get(STORAGE_ID);
        String key = player.getUUID().toString() + "." + stage;
        return data.getBooleanOr(key, false);
    }

    private static void setStage(ServerPlayer player, String stage) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        CompoundTag data = server.getCommandStorage().get(STORAGE_ID);
        String key = player.getUUID().toString() + "." + stage;
        data.putBoolean(key, true);
        server.getCommandStorage().set(STORAGE_ID, data);
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
}
