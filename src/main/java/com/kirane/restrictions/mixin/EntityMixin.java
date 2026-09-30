package com.kirane.restrictions.mixin;

import com.kirane.restrictions.MinecraftRestrictionsClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void minecraftRestrictions$blockSprint(boolean sprinting, CallbackInfo ci) {
        if (sprinting
                && (MinecraftRestrictionsClientState.mask() & 1) != 0
                && (Object) this == Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
