package de.achievementchallenge.managers;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet das Speichern und Laden aller Daten
 *
 * Speichert:
 * - Timer-Daten (Ticks, Running-Status, etc.)
 * - ActionBar-Einstellungen
 * - Challenge-Daten (Einzelkämpfer, Dauer, Status)
 * - Achievement-Fortschritt (First Completions und Spieler-Achievements)
 */
public class DataManager {

    private final AchievementChallengePlugin plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // ==================== Timer-Daten (statisch, da global) ====================
    private static long timerTicks = 0;
    private static boolean timerRunning = false;
    private static long timerStartTime = 0;

    // ==================== ActionBar-Einstellungen ====================
    private final Set<UUID> actionBarEnabled = new HashSet<>();

    public DataManager(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        setupDataFile();
    }

    /**
     * Erstellt die data.yml Datei
     */
    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte data.yml nicht erstellen!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Lädt alle Daten aus der data.yml
     */
    public void loadData() {
        // Lade Timer-Daten
        timerTicks = dataConfig.getLong("timer.ticks", 0);
        timerRunning = dataConfig.getBoolean("timer.running", false);
        timerStartTime = dataConfig.getLong("timer.startTime", 0);

        // Lade ActionBar-Einstellungen
        List<String> actionBarList = dataConfig.getStringList("actionbar.enabled");
        for (String uuidStr : actionBarList) {
            try {
                actionBarEnabled.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ungültige UUID in ActionBar-Liste: " + uuidStr);
            }
        }

        // Lade Challenge-Daten
        loadChallengeData();

        // Lade Achievement-Daten
        loadAchievementData();

        // Lade Dämon-Daten
        loadDaemonData();

        plugin.getLogger().info("Daten erfolgreich geladen");
    }

    /**
     * Lädt Challenge-Daten
     */
    private void loadChallengeData() {
        if (dataConfig.contains("challenge.einzelkaempferUUID")) {
            try {
                UUID uuid = UUID.fromString(dataConfig.getString("challenge.einzelkaempferUUID"));
                String name = dataConfig.getString("challenge.einzelkaempferName");
                long duration = dataConfig.getLong("challenge.durationTicks");
                boolean active = dataConfig.getBoolean("challenge.active");

                plugin.getChallengeManager().setData(uuid, name, duration, active);

                // Wenn Challenge aktiv ist, starte Challenge-Check
                if (active) {
                    plugin.getTimerManager().startChallengeCheck();
                    plugin.getLogger().info("Challenge wiederhergestellt: " + name + " (aktiv)");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Laden der Challenge-Daten: " + e.getMessage());
            }
        }
    }

    /**
     * Lädt Achievement-Daten
     */
    private void loadAchievementData() {
        if (!dataConfig.contains("achievements")) {
            return;
        }

        ConfigurationSection achSection = dataConfig.getConfigurationSection("achievements");
        if (achSection == null) return;

        // Lade First Completions
        for (String achievementKey : achSection.getKeys(false)) {
            String path = "achievements." + achievementKey;

            String firstCompleterStr = dataConfig.getString(path + ".firstCompleter");
            UUID firstCompleter = firstCompleterStr != null ? UUID.fromString(firstCompleterStr) : null;
            String firstCompleterName = dataConfig.getString(path + ".firstCompleterName");
            long firstCompletionTime = dataConfig.getLong(path + ".firstCompletionTime", 0);
            long timerTicksAtCompletion = dataConfig.getLong(path + ".timerTicksAtCompletion", 0);

            if (firstCompleter != null) {
                AchievementProgressManager.AchievementData data = new AchievementProgressManager.AchievementData(
                        firstCompleter, timerTicksAtCompletion, firstCompleterName, firstCompletionTime
                );
                plugin.getAchievementProgressManager().getAllFirstCompletions().put(achievementKey, data);
            }

            // Lade Spieler-Achievements für dieses Achievement
            List<String> completers = dataConfig.getStringList(path + ".completers");
            for (String completerStr : completers) {
                try {
                    UUID completerId = UUID.fromString(completerStr);
                    plugin.getAchievementProgressManager()
                            .getPlayerCompletedAchievementsInternal(completerId)
                            .add(achievementKey);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Ungültige UUID bei Achievement-Completer: " + completerStr);
                }
            }
        }

        plugin.getLogger().info("Achievement-Daten geladen: " +
                plugin.getAchievementProgressManager().getAllFirstCompletions().size() + " Achievements");
    }

    /**
     * Speichert alle Daten in die data.yml
     */
    public void saveData() {
        // Speichere Timer-Daten
        dataConfig.set("timer.ticks", timerTicks);
        dataConfig.set("timer.running", timerRunning);
        dataConfig.set("timer.startTime", timerStartTime);

        // Speichere ActionBar-Einstellungen
        List<String> actionBarList = new ArrayList<>();
        for (UUID uuid : actionBarEnabled) {
            actionBarList.add(uuid.toString());
        }
        dataConfig.set("actionbar.enabled", actionBarList);

        // Speichere Challenge-Daten
        saveChallengeData();

        // Speichere Achievement-Daten
        saveAchievementData();

        // Speichere Dämon-Daten
        saveDaemonData();

        // Schreibe in Datei
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Daten!");
            e.printStackTrace();
        }
    }

    /**
     * Speichert Challenge-Daten
     */
    private void saveChallengeData() {
        ChallengeManager cm = plugin.getChallengeManager();

        if (cm.getEinzelkaempferUUID() != null) {
            dataConfig.set("challenge.einzelkaempferUUID", cm.getEinzelkaempferUUID().toString());
            dataConfig.set("challenge.einzelkaempferName", cm.getEinzelkaempferName());
            dataConfig.set("challenge.durationTicks", cm.getChallengeDurationTicks());
            dataConfig.set("challenge.active", cm.isChallengeActive());
        } else {
            dataConfig.set("challenge", null); // Löschen wenn keine Challenge aktiv
        }
    }

    /**
     * Speichert Achievement-Daten
     */
    private void saveAchievementData() {
        dataConfig.set("achievements", null); // Leere alte Achievement-Daten

        Map<String, AchievementProgressManager.AchievementData> firstCompletions =
                plugin.getAchievementProgressManager().getAllFirstCompletions();

        for (Map.Entry<String, AchievementProgressManager.AchievementData> entry : firstCompletions.entrySet()) {
            String path = "achievements." + entry.getKey();
            AchievementProgressManager.AchievementData data = entry.getValue();

            // Speichere First Completion
            dataConfig.set(path + ".firstCompleter", data.getFirstCompleter().toString());
            dataConfig.set(path + ".firstCompleterName", data.getFirstCompleterName());
            dataConfig.set(path + ".firstCompletionTime", data.getFirstCompletionTime());
            dataConfig.set(path + ".timerTicksAtCompletion", data.getTimerTicksAtCompletion());

            // Speichere alle Spieler, die dieses Achievement haben
            List<String> completerUUIDs = new ArrayList<>();
            Map<UUID, Set<String>> allPlayerCompletions =
                    plugin.getAchievementProgressManager().getAllPlayerCompletions();

            for (Map.Entry<UUID, Set<String>> playerEntry : allPlayerCompletions.entrySet()) {
                if (playerEntry.getValue().contains(entry.getKey())) {
                    completerUUIDs.add(playerEntry.getKey().toString());
                }
            }

            dataConfig.set(path + ".completers", completerUUIDs);
        }
    }

    /**
     * Speichert Dämon-Daten
     */
    private void saveDaemonData() {
        dataConfig.set("daemons", null); // Leere alte Daten

        Map<UUID, String> daemons = plugin.getDaemonManager().getAllDaemons();

        for (Map.Entry<UUID, String> entry : daemons.entrySet()) {
            dataConfig.set("daemons." + entry.getKey().toString(), entry.getValue());
        }
    }

    // ==================== Timer-Methoden ====================

    public static long getTimerTicks() {
        if (timerRunning) {
            // Berechne die verstrichene Zeit seit Start und addiere sie
            long elapsed = (System.currentTimeMillis() - timerStartTime) / 50;
            return timerTicks + elapsed;
        }
        return timerTicks;
    }

    public void setTimerTicks(long ticks) {
        timerTicks = ticks;
        if (timerRunning) {
            timerStartTime = System.currentTimeMillis();
        }
    }

    public boolean isTimerRunning() {
        return timerRunning;
    }

    public void startTimer() {
        if (!timerRunning) {
            timerRunning = true;
            timerStartTime = System.currentTimeMillis();
        }
    }

    public void pauseTimer() {
        if (timerRunning) {
            timerTicks = getTimerTicks(); // Speichere aktuellen Stand
            timerRunning = false;
        }
    }

    public void resetTimer() {
        timerTicks = 0;
        timerRunning = false;
        timerStartTime = 0;
    }

    // ==================== ActionBar-Methoden ====================

    public boolean hasActionBarEnabled(UUID uuid) {
        return actionBarEnabled.contains(uuid);
    }

    public void toggleActionBar(UUID uuid) {
        if (actionBarEnabled.contains(uuid)) {
            actionBarEnabled.remove(uuid);
        } else {
            actionBarEnabled.add(uuid);
        }
    }

    // ==================== Hilfsmethoden ====================

    /**
     * Formatiert Ticks in einen lesbaren Zeit-String
     */
    public static String formatTime(long ticks) {
        long seconds = ticks / 20;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");

        return sb.toString();
    }
}