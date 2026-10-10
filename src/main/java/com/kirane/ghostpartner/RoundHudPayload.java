package com.kirane.ghostpartner;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server-authoritative snapshot for the client HUD.
 * Keep visual state in this payload; never put round rules in client rendering code.
 */
public record RoundHudPayload(
        boolean active,
        byte role,
        int remainingSeconds,
        BlockPos target,
        boolean targetInSameDimension,
        int ritualTicks,
        int ritualTotalTicks
) implements CustomPacketPayload {
    public static final Type<RoundHudPayload> TYPE =
            new Type<>(Identifier.parse(GhostPartnerMod.MOD_ID + ":round_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoundHudPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RoundHudPayload::active,
            ByteBufCodecs.BYTE, RoundHudPayload::role,
            ByteBufCodecs.VAR_INT, RoundHudPayload::remainingSeconds,
            BlockPos.STREAM_CODEC, RoundHudPayload::target,
            ByteBufCodecs.BOOL, RoundHudPayload::targetInSameDimension,
            ByteBufCodecs.VAR_INT, RoundHudPayload::ritualTicks,
            ByteBufCodecs.VAR_INT, RoundHudPayload::ritualTotalTicks,
            RoundHudPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
