package com.kirane.ghostpartner;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Relative;
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
    private static final double RITUAL_RADIUS = 2.0;

    @Override
    public void onInitialize() {
        GhostPartnerConfig.load();
        PayloadTypeRegistry.clientboundPlay().register(RoundHudPayload.TYPE, RoundHudPayload.CODEC);
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
            data.putLong("deadline", System.currentTimeMillis() + GhostPartnerConfig.timerSeconds * 1000L);
            data.putInt("ritualTicks", 0);
            data.putDouble("lastX", partner.getX());
            data.putDouble("lastY", partner.getY());
            data.putDouble("lastZ", partner.getZ());
            data.putFloat("lastHealth", partner.getHealth());
            save(server, data);

            partner.sendSystemMessage(Component.literal("Напарник погиб. У тебя " + GhostPartnerConfig.timerSeconds + " секунд, чтобы воскресить его!"));
            dead.sendSystemMessage(Component.literal("Ты стал призраком. Разведывай путь и помоги напарнику добраться до маяка."));
        });

        ServerTickEvents.END_SERVER_TICK.register(GhostPartnerMod::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            CompoundTag data = data(server);
            String status = data.getStringOr(STATUS, IDLE);
            if (LOST.equals(status)) {
                handler.getPlayer().setGameMode(GameType.SPECTATOR);
                handler.getPlayer().sendSystemMessage(Component.literal("Раунд уже проигран. Оператор может сбросить тест командой /ghostpartner reset."));
                syncHud(handler.getPlayer(), data, server);
                return;
            }
            if (!ACTIVE.equals(status)) {
                syncHud(handler.getPlayer(), data, server);
                return;
            }
            UUID ghostId = uuid(data.getStringOr("ghost", ""));
            if (ghostId != null && ghostId.equals(handler.getPlayer().getUUID())) {
                prepareGhostMovement(handler.getPlayer());
            }
            syncHud(handler.getPlayer(), data, server);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));

        LOGGER.info("Ghost Partner initialized for Minecraft 26.3.");
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ghostpartner")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .then(Commands.literal("status").executes(context -> {
                    CompoundTag data = data(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Ghost Partner state: " + data.getStringOr(STATUS, IDLE)), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> resetRound(context.getSource().getServer(), context.getSource())))
                .then(Commands.literal("restart").executes(context -> resetRound(context.getSource().getServer(), context.getSource()))));
    }


    private static int resetRound(MinecraftServer server, CommandSourceStack source) {
        CompoundTag data = data(server);
        data.putString(STATUS, IDLE);
        data.remove("ghost");
        data.remove("survivor");
        data.remove("deadline");
        data.remove("ritualTicks");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.gameMode() == GameType.SPECTATOR || player.gameMode() == GameType.ADVENTURE) {
                player.setGameMode(GameType.SURVIVAL);
            }
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        syncHud(server, data);
        save(server, data);
        source.sendSuccess(() -> Component.literal("Тестовый раунд перезапущен. Мир пересоздавать не нужно."), true);
        return 1;
    }

    private static void tick(MinecraftServer server) {
        CompoundTag data = data(server);
        String status = data.getStringOr(STATUS, IDLE);
        if (LOST.equals(status)) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.gameMode() != GameType.SPECTATOR) player.setGameMode(GameType.SPECTATOR);
            }
            return;
        }
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
        if (ghost != null) {
            prepareGhostMovement(ghost);
        }
        if (survivor == null) return;

        ServerLevel beaconLevel = findDeathLevel(server, data);
        if (beaconLevel != null) {
            double x = data.getDoubleOr("deathX", 0.0);
            double y = data.getDoubleOr("deathY", 0.0);
            double z = data.getDoubleOr("deathZ", 0.0);
            if (GhostPartnerConfig.soulParticles && server.getTickCount() % 5 == 0) {
                beaconLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y + 0.5, z,
                        3, 0.22, 0.35, 0.22, 0.01);
                beaconLevel.sendParticles(ParticleTypes.SOUL, x, y + 0.7, z,
                        1, 0.18, 0.25, 0.18, 0.005);
            }
            protectNearbyLoot(beaconLevel, x, y, z);
        }

        if (ghost != null && ghost.level() == survivor.level()) {
            keepGhostNear(ghost, survivor);
            if (GhostPartnerConfig.soulParticles && server.getTickCount() % 10 == 0) {
                ghost.level().sendParticles(ParticleTypes.SOUL, ghost.getX(), ghost.getY() + 0.5,
                        ghost.getZ(), 2, 0.25, 0.35, 0.25, 0.005);
            }
        }

        if (server.getTickCount() % 5 == 0) {
            syncHud(server, data);
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
        boolean holding = survivor.isShiftKeyDown();

        int progress = data.getIntOr("ritualTicks", 0);
        if (atBeacon && holding && !moved && !damaged) {
            progress++;
        } else {
            progress = 0;
        }
        data.putInt("ritualTicks", progress);
        data.putDouble("lastX", survivor.getX());
        data.putDouble("lastY", survivor.getY());
        data.putDouble("lastZ", survivor.getZ());
        data.putFloat("lastHealth", survivor.getHealth());

        int ritualGoal = GhostPartnerConfig.ritualSeconds * 20;
        if (ghost == null && progress >= ritualGoal) {
            // Do not display a completed ritual when the player to resurrect is offline.
            data.putInt("ritualTicks", Math.max(0, ritualGoal - 1));
            return;
        }
        if (progress < ritualGoal) return;

        ServerLevel deathLevel = findDeathLevel(server, data);
        if (deathLevel == null) {
            loseRound(server, "Измерение маяка недоступно. Раунд остановлен.");
            return;
        }

        double x = data.getDoubleOr("deathX", 0.0) + 0.5;
        double y = data.getDoubleOr("deathY", 0.0) + 0.1;
        double z = data.getDoubleOr("deathZ", 0.0) + 0.5;
        if (ghost != null) {
            ghost.teleportTo(deathLevel, x, y, z, Relative.ROTATION, ghost.getYRot(), ghost.getXRot(), true);
            ghost.setGameMode(GameType.SURVIVAL);
            ghost.getAbilities().mayfly = false;
            ghost.getAbilities().flying = false;
            ghost.onUpdateAbilities();
            ghost.setHealth((float) GhostPartnerConfig.reviveHealth);
            ghost.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, GhostPartnerConfig.resistanceSeconds * 20, 0));
            ghost.sendSystemMessage(Component.literal("Ты воскрес! У вас снова одна общая попытка."));
        }

        data.putString(STATUS, IDLE);
        data.remove("ghost");
        data.remove("survivor");
        data.remove("deadline");
        data.remove("ritualTicks");
        survivor.sendSystemMessage(Component.literal("Напарник воскрешён! Раунд продолжается."));
        syncHud(server, data);
        save(server, data);
    }

    /**
     * Sends authoritative round state to clients. Rendering never owns game rules:
     * it only displays this snapshot and can later be replaced without touching mechanics.
     */
    private static void syncHud(MinecraftServer server, CompoundTag data) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncHud(player, data, server);
        }
    }

    private static void syncHud(ServerPlayer player, CompoundTag data, MinecraftServer server) {
        String status = data.getStringOr(STATUS, IDLE);
        if (ACTIVE.equals(status)) {
            UUID ghostId = uuid(data.getStringOr("ghost", ""));
            UUID survivorId = uuid(data.getStringOr("survivor", ""));
            byte role = ghostId != null && ghostId.equals(player.getUUID()) ? (byte) 2
                    : survivorId != null && survivorId.equals(player.getUUID()) ? (byte) 1 : (byte) 0;
            int seconds = (int) Math.max(0,
                    (data.getLongOr("deadline", 0L) - System.currentTimeMillis() + 999L) / 1000L);
            BlockPos target = BlockPos.containing(
                    data.getDoubleOr("deathX", 0.0),
                    data.getDoubleOr("deathY", 0.0),
                    data.getDoubleOr("deathZ", 0.0));
            ServerPlayNetworking.send(player, new RoundHudPayload(
                    true, role, seconds, target,
                    player.level().dimension().toString().equals(data.getStringOr("deathDimension", "")),
                    data.getIntOr("ritualTicks", 0), GhostPartnerConfig.ritualSeconds * 20));
        } else if (LOST.equals(status)) {
            ServerPlayNetworking.send(player, new RoundHudPayload(
                    false, (byte) 3, 0, BlockPos.ZERO, false, 0, GhostPartnerConfig.ritualSeconds * 20));
        } else {
            ServerPlayNetworking.send(player, new RoundHudPayload(
                    false, (byte) 0, 0, BlockPos.ZERO, false, 0, GhostPartnerConfig.ritualSeconds * 20));
        }
    }

    private static void prepareGhostMovement(ServerPlayer ghost) {
        if (ghost.gameMode() != GameType.ADVENTURE) {
            ghost.setGameMode(GameType.ADVENTURE);
        }
        // Flight remains available, but Adventure mode keeps normal block collision and line of sight.
        ghost.getAbilities().mayfly = true;
        ghost.getAbilities().flying = true;
        ghost.onUpdateAbilities();
    }

    private static void keepGhostNear(ServerPlayer ghost, ServerPlayer survivor) {
        double dx = ghost.getX() - survivor.getX();
        double dy = ghost.getY() - survivor.getY();
        double dz = ghost.getZ() - survivor.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double radius = GhostPartnerConfig.ghostRadius;
        if (distance <= radius || distance < 0.001) return;

        // A hard spherical tether: clamp to the boundary every tick instead of teleporting
        // toward the survivor in occasional large corrections.
        double nx = dx / distance;
        double ny = dy / distance;
        double nz = dz / distance;
        ghost.setPos(survivor.getX() + nx * radius,
                survivor.getY() + ny * radius,
                survivor.getZ() + nz * radius);

        net.minecraft.world.phys.Vec3 velocity = ghost.getDeltaMovement();
        double outwardSpeed = velocity.x * nx + velocity.y * ny + velocity.z * nz;
        if (outwardSpeed > 0.0) {
            ghost.setDeltaMovement(velocity.x - nx * outwardSpeed,
                    velocity.y - ny * outwardSpeed,
                    velocity.z - nz * outwardSpeed);
        }
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
            player.sendSystemMessage(Component.literal(message), false);
            player.setGameMode(GameType.SPECTATOR);
        }
        syncHud(server, data);
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
