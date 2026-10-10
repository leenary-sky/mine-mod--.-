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

/**
 * Draws the ghost on the living partner's player renderer, so it is rendered where the camera is.
 * The local dead/ghost player's renderer can be outside the view frustum and is not a reliable host.
 */
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
                || state.id == mc.player.getId()) {
            return;
        }

        Player partner = null;
        for (Player candidate : mc.level.players()) {
            if (candidate.getId() == state.id && candidate.isAlive()) {
                partner = candidate;
                break;
            }
        }
        if (partner == null) return;

        // Orbit in the partner model's local space: radius ~0.55 blocks, gentle vertical bob.
        // 16 model pixels = 1 block; the cube itself remains 6x6x6 pixels as authored in Blockbench.
        double time = System.currentTimeMillis() / 1000.0;
        float orbitX = (float) (Math.cos(time * 1.35) * 0.55);
        float orbitZ = (float) (Math.sin(time * 1.35) * 0.55);
        float bob = (float) (Math.sin(time * 2.4) * 0.12);

        poseStack.pushPose();
        poseStack.translate(orbitX, bob, orbitZ);
        ghostModel.setupAnim(state);
        collector.submitModel(ghostModel, state, poseStack, TEXTURE, light,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
