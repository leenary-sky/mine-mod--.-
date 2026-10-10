package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class GhostCameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void ghostPartner$orbitCamera(BlockGetter level, Entity entity, boolean thirdPerson,
                                          boolean inverseView, float partialTick, CallbackInfo ci) {
        GhostPartnerClient.applyOrbitCamera((Camera) (Object) this);
    }
}
