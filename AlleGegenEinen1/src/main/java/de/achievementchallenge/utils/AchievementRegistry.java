package de.achievementchallenge.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class AchievementRegistry {

    private static final Map<String, AchievementInfo> ACHIEVEMENTS = new LinkedHashMap<>();

    public static void initialize(File dataFolder) {
        File achFile = new File(dataFolder, "trackedAchievements.yml");

        if (!achFile.exists()) {
            System.err.println("⚠️ trackedAchievements.yml nicht gefunden! Bitte Datei manuell erstellen.");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(achFile);
        ACHIEVEMENTS.clear();

        if (!config.isConfigurationSection("achievements")) {
            System.err.println("⚠️ Keine Achievements in trackedAchievements.yml gefunden!");
            return;
        }

        for (String key : config.getConfigurationSection("achievements").getKeys(false)) {
            try {
                String title = config.getString("achievements." + key + ".title", key);
                String description = config.getString("achievements." + key + ".description", "");
                String iconStr = config.getString("achievements." + key + ".icon");
                String categoryStr = config.getString("achievements." + key + ".category");

                Material icon = Material.matchMaterial(iconStr);
                if (icon == null) {
                    System.err.println("⚠️ Ungültiges Icon-Material für Achievement: " + key + " -> " + iconStr);
                    continue;
                }

                AchievementCategory category;
                try {
                    category = AchievementCategory.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("⚠️ Ungültige Kategorie für Achievement: " + key + " -> " + categoryStr);
                    continue;
                }

                ACHIEVEMENTS.put(key, new AchievementInfo(key, title, description, icon, category));
            } catch (Exception e) {
                System.err.println("⚠️ Fehler beim Einlesen eines Achievements: " + key + " — " + e.getMessage());
            }
        }

        System.out.println("✅ " + ACHIEVEMENTS.size() + " Achievements aus trackedAchievements.yml geladen.");
    }

    public static Map<String, AchievementInfo> getAchievements() {
        return Collections.unmodifiableMap(ACHIEVEMENTS);
    }

    public static List<String> getAchievementKeys() {
        return new ArrayList<>(ACHIEVEMENTS.keySet());
    }

    public static AchievementInfo getAchievementInfo(String key) {
        return ACHIEVEMENTS.get(key);
    }

    public static boolean isTrackedAchievement(String key) {
        return ACHIEVEMENTS.containsKey(key);
    }

    public static int getTotalAchievementCount() {
        return ACHIEVEMENTS.size();
    }

    public static List<String> getAchievementsByCategory(AchievementCategory category) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, AchievementInfo> entry : ACHIEVEMENTS.entrySet()) {
            if (entry.getValue().getCategory() == category) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public enum AchievementCategory {
        STORY("Story"),
        NETHER("Nether"),
        END("The End"),
        ADVENTURE("Adventure"),
        HUSBANDRY("Husbandry");

        private final String displayName;

        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static class AchievementInfo {
        private final String key;
        private final String title;
        private final String description;
        private final Material icon;
        private final AchievementCategory category;

        public AchievementInfo(String key, String title, String description, Material icon, AchievementCategory category) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.category = category;
        }

        public String getKey() {
            return key;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Material getIcon() {
            return icon;
        }

        public AchievementCategory getCategory() {
            return category;
        }
    }
}