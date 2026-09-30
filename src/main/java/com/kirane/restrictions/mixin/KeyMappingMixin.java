package com.kirane.restrictions.mixin;

import com.kirane.restrictions.MinecraftRestrictionsClientState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void minecraftRestrictions$blockSprintKey(boolean value, CallbackInfo ci) {
        if (value
                && (MinecraftRestrictionsClientState.mask() & 1) != 0
                && (Object) this == Minecraft.getInstance().options.keySprint) {
            ci.cancel();
        }
    }
}
