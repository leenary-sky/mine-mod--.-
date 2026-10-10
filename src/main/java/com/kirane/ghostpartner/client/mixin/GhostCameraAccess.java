package com.kirane.ghostpartner.client.mixin;

import net.minecraft.world.phys.Vec3;

public interface GhostCameraAccess {
    void ghostPartner$setPosition(Vec3 position);
    void ghostPartner$setRotation(float yaw, float pitch);
}
