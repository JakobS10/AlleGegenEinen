package de.achievementchallenge.listeners;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener für Achievement-Events
 *
 * Reagiert auf:
 * - PlayerAdvancementDoneEvent: Wenn ein Spieler ein Achievement abschließt
 * - PlayerJoinEvent: Update der Achievements beim Join
 */
public class AchievementListener implements Listener {

    private final AchievementChallengePlugin plugin;

    public AchievementListener(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Wird aufgerufen, wenn ein Spieler ein Achievement abschließt
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        // Async verarbeiten um Lag zu vermeiden
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getAchievementProgressManager().checkAndRecordAchievement(player, advancement);

            // Zurück zum Main Thread für das Speichern
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getDataManager().saveData();
            });
        });
    }

    /**
     * Wird aufgerufen, wenn ein Spieler dem Server beitritt
     * Update Achievements mit Delay (10 Sekunden nach Join)
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update Achievements mit Delay (10 Sekunden nach Join)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getAchievementProgressManager().updatePlayerAchievements(player);
        }, 200L);
    }
}