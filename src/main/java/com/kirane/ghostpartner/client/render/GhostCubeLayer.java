package com.kirane.ghostpartner.client.render;

import com.kirane.ghostpartner.client.GhostPartnerClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Renders the local dead player's small ghost cube beside the surviving partner. */
public final class GhostCubeLayer extends RenderLayer<AvatarRenderState, net.minecraft.client.model.player.PlayerModel> {
    private static final Identifier TEXTURE =
            Identifier.parse("ghost_partner:textures/entity/ghost.png");
    private final GhostCubeModel ghostModel =
            new GhostCubeModel(GhostCubeModel.createLayer().bakeRoot());

    public GhostCubeLayer(RenderLayerParent<AvatarRenderState, net.minecraft.client.model.player.PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yRot, float xRot) {
        Minecraft mc = Minecraft.getInstance();
        if (!GhostPartnerClient.isGhostCameraActive() || mc.player == null || mc.level == null
                || state.id != mc.player.getId()) {
            return;
        }

        Player target = null;
        for (Player candidate : mc.level.players()) {
            if (candidate != mc.player && candidate.isAlive()) {
                target = candidate;
                break;
            }
        }
        if (target == null) return;

        Vec3 local = mc.player.position();
        Vec3 targetPos = target.position();
        poseStack.pushPose();
        poseStack.translate(targetPos.x - local.x, targetPos.y - local.y, targetPos.z - local.z);
        ghostModel.setupAnim(state);
        collector.submitModel(ghostModel, state, poseStack, TEXTURE, light,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
