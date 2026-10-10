package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.hud.Hud;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class GhostHudMixin {
    /**
     * Minecraft 26.3 moved the vanilla status-bar rendering into Hud.
     * Skip this grouped pass for ghosts to hide the hotbar, health, armor,
     * hunger and experience/contextual bar without relying on removed Gui methods.
     */
    @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideGhostHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (GhostPartnerClient.isGhostCameraActive()) {
            ci.cancel();
        }
    }
}
