

package de.achievementchallenge.managers;

import de.achievementchallenge.AchievementChallengePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Verwaltet den Timer und prüft, ob die Challenge-Zeit abgelaufen ist
 *
 * WICHTIG: Diese Klasse wurde erweitert, um die Challenge-Integration zu unterstützen!
 */
public class TimerManager {

    private final AchievementChallengePlugin plugin;

    // Task-ID für die Challenge-Prüfung
    private int challengeCheckTaskId = -1;

    public TimerManager(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Startet die Challenge-Prüfung (wird aufgerufen, wenn Challenge gestartet wird)
     * Prüft jede Sekunde, ob die Challenge-Zeit abgelaufen ist
     */
    public void startChallengeCheck() {
        // Stoppe alte Task falls vorhanden
        if (challengeCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(challengeCheckTaskId);
        }

        // Starte neue Task (alle 20 Ticks = 1 Sekunde)
        challengeCheckTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkChallengeTime();
        }, 20L, 20L).getTaskId();

        plugin.getLogger().info("Challenge-Check gestartet");
    }

    /**
     * Stoppt die Challenge-Prüfung
     */
    public void stopChallengeCheck() {
        if (challengeCheckTaskId != -1) {
            Bukkit.getScheduler().cancelTask(challengeCheckTaskId);
            challengeCheckTaskId = -1;
            plugin.getLogger().info("Challenge-Check gestoppt");
        }
    }

    /**
     * Prüft, ob die Challenge-Zeit abgelaufen ist
     */
    private void checkChallengeTime() {
        ChallengeManager cm = plugin.getChallengeManager();

        // Prüfe ob Challenge aktiv ist
        if (!cm.isChallengeActive()) {
            stopChallengeCheck();
            return;
        }

        // Hole aktuelle und Ziel-Zeit
        DataManager dm = plugin.getDataManager();
        long currentTicks = dm.getTimerTicks();
        long targetTicks = cm.getChallengeDurationTicks();

        // Prüfe ob Zeit abgelaufen ist
        if (currentTicks >= targetTicks) {
            // Challenge beenden
            plugin.getLogger().info("Challenge-Zeit abgelaufen! Rufe completed() auf...");
            cm.completed();
            stopChallengeCheck();
        }
    }

    /**
     * Aktualisiert die ActionBars für alle Spieler
     */
    public void updateActionBars() {
        DataManager dm = plugin.getDataManager();
        String timeStr = DataManager.formatTime(dm.getTimerTicks());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (dm.hasActionBarEnabled(player.getUniqueId())) {
                player.sendActionBar(Component.text("§e" + timeStr));
            }
        }
    }
}