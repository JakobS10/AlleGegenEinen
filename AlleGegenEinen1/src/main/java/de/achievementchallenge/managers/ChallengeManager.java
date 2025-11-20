package de.achievementchallenge.managers;

import org.bukkit.Bukkit;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet die Challenge zwischen Einzelkämpfer und allen anderen Spielern
 * <p>
 * Aufgaben:
 * - Einzelkämpfer setzen und verwalten
 * - Challenge-Zeit festlegen und Timer steuern
 * - Fortschritt berechnen (Einzelkämpfer vs. Alle anderen)
 * - Challenge beenden und Gewinner ermitteln
 */
public class ChallengeManager {

    private final AchievementChallengePlugin plugin;

    // Der UUID des Einzelkämpfers (null = keine aktive Challenge)
    private UUID einzelkaempferUUID;

    // Name des Einzelkämpfers (für Anzeige auch wenn offline)
    private String einzelkaempferName;

    // Challenge-Zeit in Ticks (wird beim Setzen des Einzelkämpfers festgelegt)
    private long challengeDurationTicks;

    // Ist die Challenge aktiv?
    private boolean challengeActive;

    public ChallengeManager(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        this.einzelkaempferUUID = null;
        this.einzelkaempferName = null;
        this.challengeDurationTicks = 0;
        this.challengeActive = false;
    }

    /**
     * Setzt den Einzelkämpfer und startet die Challenge
     *
     * @param player  Der Spieler, der Einzelkämpfer werden soll
     * @param days    Tage für die Challenge-Dauer
     * @param hours   Stunden für die Challenge-Dauer
     * @param minutes Minuten für die Challenge-Dauer
     * @param seconds Sekunden für die Challenge-Dauer
     */
    public void setEinzelkaempfer(Player player, int days, int hours, int minutes, int seconds) {
        // Speichere Einzelkämpfer-Daten
        this.einzelkaempferUUID = player.getUniqueId();
        this.einzelkaempferName = player.getName();

        // Berechne Challenge-Dauer in Ticks
        long totalSeconds = (days * 86400L) + (hours * 3600L) + (minutes * 60L) + seconds;
        this.challengeDurationTicks = totalSeconds * 20L;

        // Aktiviere Challenge
        this.challengeActive = true;

        // Reset aller Achievements und Inventare
        resetAllAchievementsAndInventories();

        // Starte Timer bei 0
        DataManager dm = plugin.getDataManager();
        dm.setTimerTicks(0);
        dm.startTimer();
        dm.saveData();

        // Broadcast an alle Spieler
        String timeStr = DataManager.formatTime(challengeDurationTicks);
        Bukkit.broadcastMessage("§6§l=== CHALLENGE GESTARTET ===");
        Bukkit.broadcastMessage("§e" + player.getName() + " §7ist jetzt der §c§lEinzelkämpfer§7!");
        Bukkit.broadcastMessage("§7Ziel: Mehr Achievements schaffen als alle anderen zusammen!");
        Bukkit.broadcastMessage("§7Zeit: §e" + timeStr);
        Bukkit.broadcastMessage("§6§l========================");

        // Speichere alles
        plugin.getDataManager().saveData();

        plugin.getLogger().info("Challenge gestartet: " + player.getName() + " für " + timeStr);
    }

    /**
     * Löscht alle Achievements von allen Spielern (sowohl im Spiel als auch im Plugin)
     * und leert alle Inventare
     */
    private void resetAllAchievementsAndInventories() {
        // Reset Achievement-Tracking im Plugin
        plugin.getAchievementProgressManager().clearData();

        // Entferne Achievements von allen ONLINE Spielern
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Revoke alle getrackten Achievements
            revokeAllTrackedAchievements(p);

            // Leere Inventar
            p.getInventory().clear();
            p.getEnderChest().clear();

            // Setze Erfahrung zurück
            p.setLevel(0);
            p.setExp(0);

            p.sendMessage("§cDein Inventar und alle Achievements wurden zurückgesetzt!");
        }

        plugin.getLogger().info("Alle Achievements und Inventare wurden zurückgesetzt");
    }

    /**
     * Entfernt alle getrackten Achievements von einem Spieler
     *
     * @param player Der Spieler
     * @return Anzahl der entfernten Achievements
     */
    private int revokeAllTrackedAchievements(Player player) {
        int count = 0;

        // Hole alle getrackten Achievement-Keys aus dem Registry
        for (String key : de.achievementchallenge.utils.AchievementRegistry.getAchievementKeys()) {
            try {
                // Erstelle den NamespacedKey
                org.bukkit.NamespacedKey namespacedKey = org.bukkit.NamespacedKey.minecraft(key);

                // Hole das Advancement
                org.bukkit.advancement.Advancement advancement = Bukkit.getAdvancement(namespacedKey);

                if (advancement != null) {
                    // Hole den Progress des Spielers
                    org.bukkit.advancement.AdvancementProgress progress = player.getAdvancementProgress(advancement);

                    // Wenn der Spieler das Achievement hat, entferne es
                    if (progress.isDone()) {
                        // Entferne alle gewährten Kriterien
                        for (String criteria : progress.getAwardedCriteria()) {
                            progress.revokeCriteria(criteria);
                        }
                        count++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Fehler beim Entfernen von Achievement " + key + ": " + e.getMessage());
            }
        }

        return count;
    }

    /**
     * Wird vom Timer aufgerufen, wenn die Challenge-Zeit abgelaufen ist
     * Ermittelt den Gewinner und beendet die Challenge
     */
    public void completed() {
        if (!challengeActive) {
            plugin.getLogger().warning("completed() wurde aufgerufen, aber keine Challenge ist aktiv!");
            return;
        }

        // Berechne Ergebnisse
        int einzelkaempferCount = getEinzelkaempferAchievementCount();
        int othersCount = getOthersAchievementCount();

        // Ermittle Gewinner
        boolean einzelkaempferWins = einzelkaempferCount > othersCount;

        // Broadcast Ergebnis
        Bukkit.broadcastMessage("§6§l=== CHALLENGE BEENDET ===");
        Bukkit.broadcastMessage("");

        if (einzelkaempferWins) {
            Bukkit.broadcastMessage("§a§l" + einzelkaempferName + " HAT GEWONNEN!");
            Bukkit.broadcastMessage("§7Achievements: §e" + einzelkaempferCount + " §7vs. §e" + othersCount);
        } else {
            Bukkit.broadcastMessage("§c§l" + einzelkaempferName + " HAT VERLOREN!");
            Bukkit.broadcastMessage("§7Die anderen Spieler haben gewonnen!");
            Bukkit.broadcastMessage("§7Achievements: §e" + einzelkaempferCount + " §7vs. §e" + othersCount);
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l======================");

        // Challenge deaktivieren
        challengeActive = false;

        // Timer stoppen
        plugin.getDataManager().pauseTimer();

        // Speichern
        plugin.getDataManager().saveData();

        plugin.getLogger().info("Challenge beendet. Gewinner: " + (einzelkaempferWins ? einzelkaempferName : "Alle anderen"));
    }

    /**
     * Berechnet, wie viele verschiedene Achievements der Einzelkämpfer hat
     *
     * @return Anzahl der Achievements
     */
    public int getEinzelkaempferAchievementCount() {
        if (einzelkaempferUUID == null) {
            return 0;
        }

        return plugin.getAchievementProgressManager().getCompletedCount(einzelkaempferUUID);
    }

    /**
     * Berechnet, wie viele verschiedene Achievements alle anderen zusammen haben
     * (Duplikate zählen nur einmal!)
     *
     * @return Anzahl der verschiedenen Achievements
     */
    public int getOthersAchievementCount() {
        if (einzelkaempferUUID == null) {
            return 0;
        }

        // Set für alle verschiedenen Achievements der anderen
        Set<String> othersAchievements = new HashSet<>();

        // Durchlaufe alle Online-Spieler
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Überspringe den Einzelkämpfer
            if (p.getUniqueId().equals(einzelkaempferUUID)) {
                continue;
            }

            // Füge alle Achievements dieses Spielers hinzu (Set verhindert Duplikate)
            othersAchievements.addAll(
                    plugin.getAchievementProgressManager().getPlayerCompletedAchievements(p.getUniqueId())
            );
        }

        return othersAchievements.size();
    }

    /**
     * Gibt alle verschiedenen Achievements zurück, die die anderen Spieler haben
     *
     * @return Set mit Achievement-Keys
     */
    public Set<String> getOthersAchievements() {
        if (einzelkaempferUUID == null) {
            return new HashSet<>();
        }

        Set<String> othersAchievements = new HashSet<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(einzelkaempferUUID)) {
                continue;
            }

            othersAchievements.addAll(
                    plugin.getAchievementProgressManager().getPlayerCompletedAchievements(p.getUniqueId())
            );
        }

        return othersAchievements;
    }

    /**
     * Gibt die Liste aller "anderen" Spieler zurück (ohne Einzelkämpfer)
     *
     * @return Liste der Spieler
     */
    public List<Player> getOtherPlayers() {
        List<Player> others = new ArrayList<>();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (einzelkaempferUUID != null && p.getUniqueId().equals(einzelkaempferUUID)) {
                continue;
            }
            others.add(p);
        }

        return others;
    }

    /**
     * Prüft, ob eine Challenge aktiv ist
     */
    public boolean isChallengeActive() {
        return challengeActive;
    }

    /**
     * Gibt die UUID des Einzelkämpfers zurück
     */
    public UUID getEinzelkaempferUUID() {
        return einzelkaempferUUID;
    }

    /**
     * Gibt den Namen des Einzelkämpfers zurück
     */
    public String getEinzelkaempferName() {
        return einzelkaempferName;
    }

    /**
     * Gibt die Challenge-Dauer in Ticks zurück
     */
    public long getChallengeDurationTicks() {
        return challengeDurationTicks;
    }

    /**
     * Prüft, ob ein Spieler der Einzelkämpfer ist
     */
    public boolean isEinzelkaempfer(UUID uuid) {
        return einzelkaempferUUID != null && einzelkaempferUUID.equals(uuid);
    }

    /**
     * Gibt den Einzelkämpfer als OfflinePlayer zurück (auch wenn offline)
     */
    public OfflinePlayer getEinzelkaempferOffline() {
        if (einzelkaempferUUID == null) {
            return null;
        }
        return Bukkit.getOfflinePlayer(einzelkaempferUUID);
    }

    /**
     * Setzt die Challenge-Daten (für Laden aus data.yml)
     */
    public void setData(UUID uuid, String name, long duration, boolean active) {
        this.einzelkaempferUUID = uuid;
        this.einzelkaempferName = name;
        this.challengeDurationTicks = duration;
        this.challengeActive = active;
    }

    /**
     * Reset der kompletten Challenge
     */
    public void resetChallenge() {
        this.einzelkaempferUUID = null;
        this.einzelkaempferName = null;
        this.challengeDurationTicks = 0;
        this.challengeActive = false;

        // Reset Achievements und Inventare
        resetAllAchievementsAndInventories();

        // Reset Timer
        plugin.getDataManager().resetTimer();

        // Speichern
        plugin.getDataManager().saveData();

        Bukkit.broadcastMessage("§c§lChallenge wurde zurückgesetzt!");
    }
}
