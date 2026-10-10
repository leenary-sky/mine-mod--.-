package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import com.kirane.ghostpartner.client.GhostPartnerClient.CameraAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class GhostCameraMixin implements CameraAccess {
    @org.spongepowered.asm.mixin.Shadow protected abstract void setPosition(Vec3 position);
    @org.spongepowered.asm.mixin.Shadow protected abstract void setRotation(float yaw, float pitch);

    @Override public void ghostPartner$setPosition(Vec3 position) { setPosition(position); }
    @Override public void ghostPartner$setRotation(float yaw, float pitch) { setRotation(yaw, pitch); }

    // Minecraft 26.3 updates the camera through update(DeltaTracker); the old setup(...)
    // entry point no longer exists in this version.
    @Inject(method = "update", at = @At("TAIL"))
    private void ghostPartner$orbitCamera(DeltaTracker deltaTracker, CallbackInfo ci) {
        GhostPartnerClient.applyOrbitCamera((Camera) (Object) this);
    }
}
