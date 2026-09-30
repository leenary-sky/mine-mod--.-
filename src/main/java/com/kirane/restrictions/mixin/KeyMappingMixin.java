package com.kirane.restrictions.mixin;

import com.kirane.restrictions.MinecraftRestrictionsClientState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    private void minecraftRestrictions$hideSprintKey(CallbackInfoReturnable<Boolean> cir) {
        if ((MinecraftRestrictionsClientState.mask() & 1) != 0
                && (Object) this == Minecraft.getInstance().options.keySprint) {
            cir.setReturnValue(false);
        }
    }
}
