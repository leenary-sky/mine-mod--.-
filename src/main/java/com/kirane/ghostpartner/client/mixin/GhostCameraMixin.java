package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class GhostCameraMixin implements GhostCameraAccess {
    @org.spongepowered.asm.mixin.Shadow protected abstract void setPosition(Vec3 position);
    @org.spongepowered.asm.mixin.Shadow protected abstract void setRotation(float yaw, float pitch);

    @Override public void ghostPartner$setPosition(Vec3 position) { setPosition(position); }
    @Override public void ghostPartner$setRotation(float yaw, float pitch) { setRotation(yaw, pitch); }
    @Inject(method = "setup", at = @At("TAIL"))
    private void ghostPartner$orbitCamera(BlockGetter level, Entity entity, boolean thirdPerson,
                                          boolean inverseView, float partialTick, CallbackInfo ci) {
        GhostPartnerClient.applyOrbitCamera((Camera) (Object) this);
    }
}
