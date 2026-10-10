package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class GhostDeathScreenMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideVanillaDeathScreen(CallbackInfo ci) {
        if (GhostPartnerClient.isGhostCameraActive()) ci.cancel();
    }
}
