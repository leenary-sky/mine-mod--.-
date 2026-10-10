package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class GhostMouseMixin {
    @Inject(method = "onScroll", at = @At("HEAD"))
    private void ghostPartner$zoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (GhostPartnerClient.isGhostCameraActive()) {
            GhostPartnerClient.adjustOrbitZoom(vertical);
        }
    }
}
