package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostCompassScreen;
import com.kirane.ghostpartner.client.GhostPartnerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the vanilla compass as the rescue device without requiring a new registry entry or texture. */
@Mixin(MultiPlayerGameMode.class)
public abstract class GhostCompassUseMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$openRescuePanel(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player != minecraft.player
                || !player.getItemInHand(hand).is(Items.COMPASS)
                || !GhostPartnerClient.shouldOpenRescueCompass()) {
            return;
        }

        minecraft.setScreen(new GhostCompassScreen());
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
