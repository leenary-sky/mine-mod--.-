package com.kirane.ghostpartner.client;

import net.minecraft.resources.Identifier;

/** Resource locations for the editable Blockbench ghost and its runtime texture. */
public final class GhostAppearance {
    public static final String BLOCKBENCH_SOURCE = "blockbench/ghost_partner.bbmodel";
    public static final Identifier TEXTURE = Identifier.parse("ghost_partner:textures/entity/ghost.png");
    public static final Identifier INVISIBLE_SKIN = Identifier.parse("ghost_partner:textures/entity/invisible.png");
    private GhostAppearance() {}
}
