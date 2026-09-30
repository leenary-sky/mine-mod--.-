package com.kirane.restrictions;

public final class MinecraftRestrictionsClientState {
    private static int mask;

    private MinecraftRestrictionsClientState() {}

    public static int mask() {
        return mask;
    }

    public static void setMask(int value) {
        mask = value;
    }
}
