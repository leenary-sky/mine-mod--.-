package com.kirane.ghostpartner;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public final class GhostPartnerMod implements ModInitializer {
    public static final String MOD_ID = "ghost_partner";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Identifier STORAGE_ID = Identifier.parse(MOD_ID + ":round_state");

    private static final String STATUS = "status";
    private static final String IDLE = "idle";
    private static final String ACTIVE = "active";
    private static final String LOST = "lost";
    private static final long DEFAULT_TIMER_MS = 4 * 60 * 1000L;
    private static final double GHOST_RADIUS = 18.0;
    private static final double RITUAL_RADIUS = 2.0;
    private static final int RITUAL_TICKS = 100;

    @Override
    public void onInitialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer dead)) return;
            MinecraftServer server = dead.level().getServer();
            if (server == null) return;
            CompoundTag data = data(server);

            if (ACTIVE.equals(data.getStringOr(STATUS, IDLE))) {
                UUID survivor = uuid(data.getStringOr("survivor", ""));
                if (survivor != null && survivor.equals(dead.getUUID())) {
                    loseRound(server, "Погиб второй игрок. Раунд проигран.");
                }
                return;
            }

            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.size() != 2) {
                dead.sendSystemMessage(Component.literal("«Призрачный напарник»: для запуска раунда нужны ровно два игрока."));
                return;
            }

            ServerPlayer partner = players.stream()
                    .filter(p -> !p.getUUID().equals(dead.getUUID()))
                    .findFirst().orElse(null);
            if (partner == null) return;

            data.putString(STATUS, ACTIVE);
            data.putString("ghost", dead.getUUID().toString());
            data.putString("survivor", partner.getUUID().toString());
            data.putDouble("deathX", dead.getX());
            data.putDouble("deathY", dead.getY());
            data.putDouble("deathZ", dead.getZ());
            data.putString("deathDimension", dead.level().dimension().toString());
            data.putLong("deadline", System.currentTimeMillis() + DEFAULT_TIMER_MS);
            data.putInt("ritualTicks", 0);
            data.putDouble("lastX", partner.getX());
            data.putDouble("lastY", partner.getY());
            data.putDouble("lastZ", partner.getZ());
            data.putFloat("lastHealth", partner.getHealth());
            save(server, data);

            partner.sendSystemMessage(Component.literal("Напарник погиб. У тебя четыре минуты, чтобы воскресить его!"));
            dead.sendSystemMessage(Component.literal("Ты стал призраком. Разведывай путь и помоги напарнику добраться до маяка."));
        });

        ServerTickEvents.END_SERVER_TICK.register(GhostPartnerMod::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CompoundTag data = data(server);
            if (!ACTIVE.equals(data.getStringOr(STATUS, IDLE))) return;
            UUID ghostId = uuid(data.getStringOr("ghost", ""));
            if (ghostId != null && ghostId.equals(handler.getPlayer().getUUID())) {
                handler.getPlayer().setGameMode(GameType.SPECTATOR);
            }
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            CompoundTag data = data(serverPlayer.level().getServer());
            if (!ACTIVE.equals(data.getStringOr(STATUS, IDLE))) return InteractionResult.PASS;
            UUID survivor = uuid(data.getStringOr("survivor", ""));
            if (survivor != null && survivor.equals(serverPlayer.getUUID())
                    && serverPlayer.getItemInHand(hand).isEmpty()
                    && nearBeacon(serverPlayer, data)) {
                data.putBoolean("rightClickHeld", true);
                save(serverPlayer.level().getServer(), data);
            }
            return InteractionResult.PASS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));

        LOGGER.info("Ghost Partner initialized for Minecraft 26.3.");
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ghostpartner")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> {
                    CompoundTag data = data(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Ghost Partner state: " + data.getStringOr(STATUS, IDLE)), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    CompoundTag data = data(context.getSource().getServer());
                    data.putString(STATUS, IDLE);
                    data.remove("ghost");
                    data.remove("survivor");
                    data.remove("deadline");
                    data.remove("ritualTicks");
                    save(context.getSource().getServer(), data);
                    context.getSource().sendSuccess(() -> Component.literal("Тестовый раунд сброшен."), true);
                    return 1;
                })));
    }

    private static void tick(MinecraftServer server) {
        CompoundTag data = data(server);
        String status = data.getStringOr(STATUS, IDLE);
        if (!ACTIVE.equals(status)) return;

        long remaining = data.getLongOr("deadline", 0L) - System.currentTimeMillis();
        if (remaining <= 0) {
            loseRound(server, "Время истекло. Напарник не спасён — раунд проигран.");
            return;
        }

        UUID ghostId = uuid(data.getStringOr("ghost", ""));
        UUID survivorId = uuid(data.getStringOr("survivor", ""));
        if (ghostId == null || survivorId == null) {
            loseRound(server, "Состояние раунда повреждено. Раунд остановлен.");
            return;
        }

        ServerPlayer ghost = server.getPlayerList().getPlayer(ghostId);
        ServerPlayer survivor = server.getPlayerList().getPlayer(survivorId);
        if (ghost != null && ghost.gameMode() != GameType.SPECTATOR) {
            ghost.setGameMode(GameType.SPECTATOR);
        }
        if (survivor == null) return;

        ServerLevel beaconLevel = findDeathLevel(server, data);
        if (beaconLevel != null) {
            double x = data.getDoubleOr("deathX", 0.0);
            double y = data.getDoubleOr("deathY", 0.0);
            double z = data.getDoubleOr("deathZ", 0.0);
            if (server.getTickCount() % 5 == 0) {
                beaconLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.5, z,
                        3, 0.22, 0.35, 0.22, 0.01);
                beaconLevel.sendParticles(ParticleTypes.SOUL, x, y + 0.7, z,
                        1, 0.18, 0.25, 0.18, 0.005);
            }
            protectNearbyLoot(beaconLevel, x, y, z);
        }

        if (ghost != null && ghost.level() == survivor.level()) {
            keepGhostNear(ghost, survivor);
            if (server.getTickCount() % 10 == 0) {
                ghost.level().sendParticles(ParticleTypes.SOUL, ghost.getX(), ghost.getY() + 0.5,
                        ghost.getZ(), 2, 0.25, 0.35, 0.25, 0.005);
            }
        }

        if (server.getTickCount() % 20 == 0) {
            int seconds = (int) Math.max(0, remaining / 1000);
            String timer = String.format("СПАСИТЕ НАПАРНИКА: %02d:%02d", seconds / 60, seconds % 60);
            survivor.displayClientMessage(Component.literal(timer + " | маяк: "
                    + (int) data.getDoubleOr("deathX", 0.0) + " "
                    + (int) data.getDoubleOr("deathY", 0.0) + " "
                    + (int) data.getDoubleOr("deathZ", 0.0)), true);
        }

        updateRitual(server, survivor, ghost, data);
        save(server, data);
    }

    private static void updateRitual(MinecraftServer server, ServerPlayer survivor,
                                     ServerPlayer ghost, CompoundTag data) {
        boolean atBeacon = nearBeacon(survivor, data);
        double dx = survivor.getX() - data.getDoubleOr("lastX", survivor.getX());
        double dy = survivor.getY() - data.getDoubleOr("lastY", survivor.getY());
        double dz = survivor.getZ() - data.getDoubleOr("lastZ", survivor.getZ());
        boolean moved = dx * dx + dy * dy + dz * dz > 0.01;
        float oldHealth = data.getFloatOr("lastHealth", survivor.getHealth());
        boolean damaged = survivor.getHealth() < oldHealth;
        boolean holding = survivor.isShiftKeyDown() || data.getBooleanOr("rightClickHeld", false);

        int progress = data.getIntOr("ritualTicks", 0);
        if (atBeacon && holding && !moved && !damaged) {
            progress++;
        } else {
            progress = 0;
        }
        data.putInt("ritualTicks", progress);
        data.putBoolean("rightClickHeld", false);
        data.putDouble("lastX", survivor.getX());
        data.putDouble("lastY", survivor.getY());
        data.putDouble("lastZ", survivor.getZ());
        data.putFloat("lastHealth", survivor.getHealth());

        if (progress > 0 && server.getTickCount() % 5 == 0) {
            int percent = Math.min(100, progress * 100 / RITUAL_TICKS);
            survivor.displayClientMessage(Component.literal("ВОСКРЕШЕНИЕ: " + percent + "%"), true);
        }
        if (progress < RITUAL_TICKS) return;

        ServerLevel deathLevel = findDeathLevel(server, data);
        if (deathLevel == null) {
            loseRound(server, "Измерение маяка недоступно. Раунд остановлен.");
            return;
        }

        double x = data.getDoubleOr("deathX", 0.0) + 0.5;
        double y = data.getDoubleOr("deathY", 0.0) + 0.1;
        double z = data.getDoubleOr("deathZ", 0.0) + 0.5;
        if (ghost != null) {
            ghost.teleportTo(deathLevel, x, y, z, ghost.getYRot(), ghost.getXRot());
            ghost.setGameMode(GameType.SURVIVAL);
            ghost.setHealth(6.0F);
            ghost.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0));
            ghost.sendSystemMessage(Component.literal("Ты воскрес! У вас снова одна общая попытка."));
        }

        data.putString(STATUS, IDLE);
        data.remove("ghost");
        data.remove("survivor");
        data.remove("deadline");
        data.remove("ritualTicks");
        survivor.sendSystemMessage(Component.literal("Напарник воскрешён! Раунд продолжается."));
        save(server, data);
    }

    private static void keepGhostNear(ServerPlayer ghost, ServerPlayer survivor) {
        double dx = ghost.getX() - survivor.getX();
        double dy = ghost.getY() - survivor.getY();
        double dz = ghost.getZ() - survivor.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance <= GHOST_RADIUS || distance < 0.001) return;

        double targetX = survivor.getX() + dx / distance * (GHOST_RADIUS - 0.75);
        double targetY = survivor.getY() + dy / distance * (GHOST_RADIUS - 0.75);
        double targetZ = survivor.getZ() + dz / distance * (GHOST_RADIUS - 0.75);
        double factor = Math.min(0.35, (distance - GHOST_RADIUS + 0.5) / distance);
        ghost.setPos(ghost.getX() + (targetX - ghost.getX()) * factor,
                ghost.getY() + (targetY - ghost.getY()) * factor,
                ghost.getZ() + (targetZ - ghost.getZ()) * factor);
    }

    private static boolean nearBeacon(ServerPlayer player, CompoundTag data) {
        if (!player.level().dimension().toString().equals(data.getStringOr("deathDimension", ""))) {
            return false;
        }
        double dx = player.getX() - data.getDoubleOr("deathX", 0.0);
        double dy = player.getY() - data.getDoubleOr("deathY", 0.0);
        double dz = player.getZ() - data.getDoubleOr("deathZ", 0.0);
        return dx * dx + dy * dy + dz * dz <= RITUAL_RADIUS * RITUAL_RADIUS;
    }

    private static ServerLevel findDeathLevel(MinecraftServer server, CompoundTag data) {
        String key = data.getStringOr("deathDimension", "");
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().toString().equals(key)) return level;
        }
        return null;
    }

    private static void protectNearbyLoot(ServerLevel level, double x, double y, double z) {
        AABB area = new AABB(x - 2, y - 2, z - 2, x + 2, y + 3, z + 2);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
            item.setUnlimitedLifetime();
        }
    }

    private static void loseRound(MinecraftServer server, String message) {
        CompoundTag data = data(server);
        if (LOST.equals(data.getStringOr(STATUS, IDLE))) return;
        data.putString(STATUS, LOST);
        save(server, data);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(Component.literal(message), false);
            player.setGameMode(GameType.SPECTATOR);
        }
        LOGGER.warn(message);
    }

    private static CompoundTag data(MinecraftServer server) {
        return server.getCommandStorage().get(STORAGE_ID);
    }

    private static void save(MinecraftServer server, CompoundTag data) {
        server.getCommandStorage().set(STORAGE_ID, data);
    }

    private static UUID uuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return null;
        }
    }
}
