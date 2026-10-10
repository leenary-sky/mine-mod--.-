package com.kirane.ghostpartner;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client request to keep the server-side ghost body on the same horizontal orbit as the camera. */
public record GhostOrbitDistancePayload(int distanceTenths) implements CustomPacketPayload {
    public static final Type<GhostOrbitDistancePayload> TYPE =
            new Type<>(Identifier.parse(GhostPartnerMod.MOD_ID + ":orbit_distance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GhostOrbitDistancePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    GhostOrbitDistancePayload::distanceTenths,
                    GhostOrbitDistancePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
