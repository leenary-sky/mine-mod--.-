package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GhostHudMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideHotbar(CallbackInfo ci) { if (GhostPartnerClient.isGhostCameraActive()) ci.cancel(); }
    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideHealth(CallbackInfo ci) { if (GhostPartnerClient.isGhostCameraActive()) ci.cancel(); }
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideFood(CallbackInfo ci) { if (GhostPartnerClient.isGhostCameraActive()) ci.cancel(); }
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideArmor(CallbackInfo ci) { if (GhostPartnerClient.isGhostCameraActive()) ci.cancel(); }
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideExperience(CallbackInfo ci) { if (GhostPartnerClient.isGhostCameraActive()) ci.cancel(); }
}
