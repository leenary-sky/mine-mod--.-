package com.kirane.ghostpartner;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class GhostPartnerConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ghost-partner.properties");

    public static int timerSeconds = 240;
    // Maximum horizontal radius shared by the ghost body and detached camera.
    public static double ghostRadius = 9.0;
    public static int ritualSeconds = 5;
    public static double reviveHealth = 6.0;
    public static int resistanceSeconds = 3;
    public static boolean soulParticles = true;
    // Reserved for a future optional integration; no voice-chat API is bundled yet.
    public static boolean simpleVoiceChatIntegration = false;
    public static boolean deleteWorldOnFailure = false;

    private GhostPartnerConfig() {}

    public static void load() {
        Properties properties = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream input = Files.newInputStream(FILE)) {
                properties.load(input);
            } catch (IOException exception) {
                GhostPartnerMod.LOGGER.warn("Не удалось прочитать конфиг Ghost Partner.", exception);
            }
        }

        timerSeconds = clamp(parseInt(properties, "timerSeconds", 240), 180, 300);
        ghostRadius = clamp(parseDouble(properties, "ghostRadius", 9.0), 4.0, 10.0);
        ritualSeconds = clamp(parseInt(properties, "ritualSeconds", 5), 1, 30);
        reviveHealth = clamp(parseDouble(properties, "reviveHealth", 6.0), 2.0, 20.0);
        resistanceSeconds = clamp(parseInt(properties, "resistanceSeconds", 3), 1, 30);
        soulParticles = Boolean.parseBoolean(properties.getProperty("soulParticles", "true"));
        simpleVoiceChatIntegration = Boolean.parseBoolean(properties.getProperty("simpleVoiceChatIntegration", "false"));
        deleteWorldOnFailure = Boolean.parseBoolean(properties.getProperty("deleteWorldOnFailure", "false"));

        properties.setProperty("timerSeconds", Integer.toString(timerSeconds));
        properties.setProperty("ghostRadius", Double.toString(ghostRadius));
        properties.setProperty("ritualSeconds", Integer.toString(ritualSeconds));
        properties.setProperty("reviveHealth", Double.toString(reviveHealth));
        properties.setProperty("resistanceSeconds", Integer.toString(resistanceSeconds));
        properties.setProperty("soulParticles", Boolean.toString(soulParticles));
        properties.setProperty("simpleVoiceChatIntegration", Boolean.toString(simpleVoiceChatIntegration));
        properties.setProperty("deleteWorldOnFailure", Boolean.toString(deleteWorldOnFailure));
        properties.setProperty("# warning", "deleteWorldOnFailure is reserved and does not delete worlds in this build.");

        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Ghost Partner configuration");
            }
        } catch (IOException exception) {
            GhostPartnerMod.LOGGER.warn("Не удалось сохранить конфиг Ghost Partner.", exception);
        }
    }

    private static int parseInt(Properties p, String key, int fallback) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static double parseDouble(Properties p, String key, double fallback) {
        try { return Double.parseDouble(p.getProperty(key, Double.toString(fallback)).trim()); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
