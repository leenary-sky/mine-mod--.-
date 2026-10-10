package com.kirane.ghostpartner.client.mixin;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import com.kirane.ghostpartner.client.render.GhostCubeLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ghostPartner$addCubeLayer(
            net.minecraft.client.renderer.entity.EntityRendererProvider.Context context,
            boolean slimSteve, CallbackInfo ci) {
        RenderLayerParent<AvatarRenderState, PlayerModel> parent =
                (RenderLayerParent<AvatarRenderState, PlayerModel>) (Object) this;
        ((LivingEntityRendererAccessor) this).ghostPartner$addLayer(new GhostCubeLayer(parent));
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void ghostPartner$hideLocalSkin(AvatarRenderState state,
            CallbackInfoReturnable<Identifier> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (GhostPartnerClient.isGhostCameraActive() && mc.player != null
                && state.id == mc.player.getId()) {
            cir.setReturnValue(Identifier.parse("ghost_partner:textures/entity/invisible.png"));
        }
    }
}
