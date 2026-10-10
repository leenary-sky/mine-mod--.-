package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class GhostDeathScreenMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideVanillaDeathScreen(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (GhostPartnerClient.isGhostCameraActive()) {
            ci.cancel();
        }
    }
}
