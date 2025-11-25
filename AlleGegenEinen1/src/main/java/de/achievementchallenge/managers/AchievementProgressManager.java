package de.achievementchallenge.managers;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.utils.AchievementRegistry;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verwaltet den Fortschritt der Achievements für alle Spieler
 *
 * Speichert:
 * - First Completions (wer hat ein Achievement als erstes geschafft)
 * - Spieler-spezifische Achievements (welche Achievements hat jeder Spieler)
 */
public class AchievementProgressManager {

    private final AchievementChallengePlugin plugin;

    // Speichert für jedes Achievement, wer es als erstes abgeschlossen hat
    private final Map<String, AchievementData> achievementData;

    // Cache: Welche Achievements hat jeder Spieler abgeschlossen?
    private final Map<UUID, Set<String>> playerCompletedCache = new ConcurrentHashMap<>();

    // Lag-Optimierung: Wann wurde der Cache für einen Spieler zuletzt aktualisiert?
    private final Map<UUID, Long> lastCacheUpdate = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 5000; // 5 Sekunden

    public AchievementProgressManager(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        this.achievementData = new ConcurrentHashMap<>();
    }

    /**
     * Wird aufgerufen wenn ein Spieler ein Achievement abschließt
     */
    public void checkAndRecordAchievement(Player player, Advancement advancement) {
        String key = advancement.getKey().getKey();

        if (!AchievementRegistry.isTrackedAchievement(key)) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // WICHTIG: Nutze IMMER den echten Namen aus Player.getName()
        // Auch wenn DisplayName im Anonym-Mode geändert ist!
        String realPlayerName = player.getName();

        // Hole oder erstelle die Achievement-Liste für diesen Spieler
        Set<String> completed = playerCompletedCache.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());

        // Füge zum Spieler-Cache hinzu (wenn noch nicht vorhanden)
        boolean newForPlayer = completed.add(key);

        // Prüfe, ob dies das erste Mal GLOBAL ist, dass dieses Achievement geschafft wurde
        boolean firstEver = achievementData.putIfAbsent(key,
                new AchievementData(playerId, DataManager.getTimerTicks(), realPlayerName, System.currentTimeMillis())) == null;

        if (firstEver) {
            // Broadcast nur wenn es wirklich das erste Mal global ist
            AchievementRegistry.AchievementInfo info = AchievementRegistry.getAchievementInfo(key);
            if (info != null) {
                String message = "§eDer Spieler §6§l" + realPlayerName + " §ewar der Erste, der §6§l" + info.getTitle() + " §e abgeschlossen hat!";
                Bukkit.broadcastMessage(message);
            }
        }
    }

    /**
     * Bulk-Update: Aktualisiert alle Achievements eines Spielers
     * Wird aufgerufen beim Join
     */
    public void updatePlayerAchievements(Player player) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUpdate = lastCacheUpdate.get(playerId);

        // Nur alle 5 Sekunden aktualisieren (Lag-Optimierung)
        if (lastUpdate != null && now - lastUpdate < CACHE_DURATION) {
            return;
        }

        lastCacheUpdate.put(playerId, now);

        // Async durchführen um Lag zu vermeiden
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Set<String> completed = ConcurrentHashMap.newKeySet();
            int foundCount = 0;
            int notFoundCount = 0;

            for (String key : AchievementRegistry.getAchievementKeys()) {
                // Der Key enthält bereits den vollständigen Pfad (z.B. "story/mine_stone")
                Advancement advancement = Bukkit.getAdvancement(org.bukkit.NamespacedKey.minecraft(key));

                if (advancement == null) {
                    notFoundCount++;
                    if (notFoundCount <= 5) {
                        plugin.getLogger().warning("Achievement nicht gefunden: minecraft:" + key);
                    }
                    continue;
                }

                foundCount++;

                // Prüfe ob Spieler das Achievement abgeschlossen hat
                if (player.getAdvancementProgress(advancement).isDone()) {
                    completed.add(key);

                    // WICHTIG: Nutze IMMER den echten Namen aus Player.getName()
                    String realPlayerName = player.getName();
                    // Nur neu aufzeichnen wenn es GLOBAL noch nicht existiert
                    achievementData.putIfAbsent(key,
                            new AchievementData(playerId, DataManager.getTimerTicks(), realPlayerName, System.currentTimeMillis()));
                }
            }

            if (notFoundCount > 5) {
                plugin.getLogger().warning("... und " + (notFoundCount - 5) + " weitere Achievements nicht gefunden");
            }

            playerCompletedCache.put(playerId, completed);
        });
    }

    /**
     * Prüft ob ein Spieler ein bestimmtes Achievement abgeschlossen hat
     */
    public boolean hasCompleted(UUID playerId, String achievementKey) {
        Set<String> completed = playerCompletedCache.get(playerId);
        return completed != null && completed.contains(achievementKey);
    }

    /**
     * Gibt die Anzahl der abgeschlossenen Achievements für einen Spieler zurück
     */
    public int getCompletedCount(UUID playerId) {
        Set<String> completed = playerCompletedCache.get(playerId);
        return completed != null ? completed.size() : 0;
    }

    /**
     * Gibt die Gesamtanzahl der mindestens einmal abgeschlossenen Achievements zurück
     */
    public int getTotalCompletedCount() {
        return achievementData.size();
    }

    /**
     * Gibt die Daten über den ersten Abschluss eines Achievements zurück
     */
    public AchievementData getFirstCompletion(String achievementKey) {
        return achievementData.get(achievementKey);
    }

    /**
     * Gibt alle First-Completion-Daten zurück
     */
    public Map<String, AchievementData> getAllFirstCompletions() {
        return achievementData;
    }

    /**
     * Gibt alle Achievements zurück, die ein Spieler abgeschlossen hat
     */
    public Set<String> getPlayerCompletedAchievements(UUID playerId) {
        return new HashSet<>(playerCompletedCache.getOrDefault(playerId, Collections.emptySet()));
    }

    /**
     * Direkter Zugriff auf den internen Cache (für DataManager beim Laden)
     */
    public Set<String> getPlayerCompletedAchievementsInternal(UUID playerId) {
        return playerCompletedCache.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
    }

    /**
     * Gibt alle Spieler-Achievements zurück (für Speicherung)
     */
    public Map<UUID, Set<String>> getAllPlayerCompletions() {
        return new HashMap<>(playerCompletedCache);
    }

    /**
     * Löscht alle Daten (für Reset)
     */
    public void clearData() {
        achievementData.clear();
        playerCompletedCache.clear();
        lastCacheUpdate.clear();
    }

    /**
     * Daten-Klasse für Achievement-Abschlüsse
     */
    public static class AchievementData {
        private final UUID firstCompleter;           // Wer hat es als erstes geschafft
        private final long timerTicksAtCompletion;   // Timer-Stand beim ersten Abschluss
        private final String firstCompleterName;     // Name des ersten Abschließers
        private final long firstCompletionTime;      // Zeitstempel (System.currentTimeMillis)

        public AchievementData(UUID firstCompleter, long timerTicksAtCompletion, String firstCompleterName, long firstCompletionTime) {
            this.firstCompleter = firstCompleter;
            this.timerTicksAtCompletion = timerTicksAtCompletion;
            this.firstCompleterName = firstCompleterName;
            this.firstCompletionTime = firstCompletionTime;
        }

        public UUID getFirstCompleter() {
            return firstCompleter;
        }

        public String getFirstCompleterName() {
            return firstCompleterName;
        }

        public long getFirstCompletionTime() {
            return firstCompletionTime;
        }

        public long getTimerTicksAtCompletion() {
            return timerTicksAtCompletion;
        }
    }
}