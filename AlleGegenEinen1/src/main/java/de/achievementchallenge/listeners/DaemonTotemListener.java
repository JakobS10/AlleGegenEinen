package de.achievementchallenge.listeners;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.commands.BefreimichCommand;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener für das Befreiungs-Totem
 *
 * Wenn ein Dämon mit dem Befreiungs-Totem stirbt (Totem poppt),
 * wird sein Dämon-Status entfernt.
 */
public class DaemonTotemListener implements Listener {

    private final AchievementChallengePlugin plugin;

    public DaemonTotemListener(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Wird aufgerufen wenn ein Totem of Undying poppt
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityResurrect(EntityResurrectEvent event) {
        // Prüfe ob es ein Spieler ist
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Prüfe ob Spieler ein Dämon ist
        if (!plugin.getDaemonManager().isDaemon(player.getUniqueId())) {
            return;
        }

        // Prüfe welches Item verwendet wurde
        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();

        boolean isBefreiungTotem = false;

        if (BefreimichCommand.isBefreiungTotem(hand)) {
            isBefreiungTotem = true;
        } else if (BefreimichCommand.isBefreiungTotem(offhand)) {
            isBefreiungTotem = true;
        }

        // Wenn es das Befreiungs-Totem war
        if (isBefreiungTotem) {
            // Entferne Dämon-Status
            plugin.getDaemonManager().removeDaemon(player.getUniqueId());

            // Lade zufällige Ankündigung
            String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("befreimich");
            announcement = announcement.replace("{player}", player.getName());

            // Broadcast
            String finalAnnouncement = announcement;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§a§l✦ BEFREIUNG ✦");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§7" + finalAnnouncement);
                Bukkit.broadcastMessage("");

                // Sound
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                }
            }, 20L); // 1 Sekunde Delay

            // Nachricht für den Ex-Dämon
            player.sendMessage("");
            player.sendMessage("§a§l✓ Du bist befreit!");
            player.sendMessage("§7Deine dunklen Kräfte sind verschwunden.");
            player.sendMessage("§7Du bist wieder ein normaler Spieler.");
            player.sendMessage("");

            // Speichere Änderungen
            plugin.getDataManager().saveData();

            plugin.getLogger().info(player.getName() + " wurde vom Dämon-Status befreit (Totem)");
        }
    }
}