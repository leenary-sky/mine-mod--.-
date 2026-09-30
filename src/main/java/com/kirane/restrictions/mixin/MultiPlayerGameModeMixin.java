package com.kirane.restrictions.mixin;

import com.kirane.restrictions.MinecraftRestrictionsClientState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void minecraftRestrictions$blockRestrictedItemUse(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {

        ItemStack stack = player.getItemInHand(hand);

        if (((MinecraftRestrictionsClientState.mask() & 2) != 0 && stack.is(Items.SHIELD))
                || ((MinecraftRestrictionsClientState.mask() & 8) != 0 && stack.is(Items.BOW))) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
