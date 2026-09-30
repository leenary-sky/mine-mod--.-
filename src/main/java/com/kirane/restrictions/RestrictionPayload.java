package com.kirane.restrictions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RestrictionPayload(int stage) implements CustomPacketPayload {
    public static final Type<RestrictionPayload> TYPE =
            new Type<>(Identifier.parse("minecraft_restrictions:restriction_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RestrictionPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RestrictionPayload::stage,
                    RestrictionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
