package com.kirane.ghostpartner.client;

import net.minecraft.resources.Identifier;

/**
 * Resource contract for the future custom ghost appearance.
 *
 * The Blockbench project is the editable source. A runtime renderer/model export still needs
 * to be selected and implemented; these identifiers are placeholders, not loaded assets yet.
 */
public final class GhostAppearance {
    public static final String BLOCKBENCH_SOURCE = "blockbench/ghost_partner.bbmodel";
    public static final Identifier TEXTURE =
            Identifier.parse("ghost_partner:textures/entity/ghost.png");
    public static final Identifier ANIMATION_DATA =
            Identifier.parse("ghost_partner:animations/ghost.animation.json");

    private GhostAppearance() {}
}
