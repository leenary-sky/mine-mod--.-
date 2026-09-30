package com.kirane.restrictions.mixin;

import com.kirane.restrictions.MinecraftRestrictions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void minecraftRestrictions$blockPlacement(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player
                && MinecraftRestrictions.hasEndRestriction(player)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
