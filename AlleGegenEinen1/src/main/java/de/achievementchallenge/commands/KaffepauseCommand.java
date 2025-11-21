package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /kaffepause
 *
 * Sendet eine zufällige Ausrede in den Chat, wartet 5 Sekunden und kickt alle Spieler.
 * Nur für Dämonen verfügbar.
 */
public class KaffepauseCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public KaffepauseCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Prüfe ob Sender ein Spieler ist
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern ausgeführt werden!");
            return true;
        }

        Player player = (Player) sender;

        // Prüfe Dämon-Status
        if (!plugin.getDaemonManager().checkDaemonPermission(player)) {
            return true;
        }

        // Starte Kaffeepause
        startKaffepause(player);

        return true;
    }

    /**
     * Startet die Kaffeepause-Sequenz
     */
    private void startKaffepause(Player executor) {
        // Lade zufällige Ausrede
        String excuse = plugin.getAnnouncementManager().getRandomAnnouncement("kaffepause");

        // Broadcast Ausrede
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§lEINEN MOMENT BITTE");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e" + excuse);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Kick in 5 Sekunden...");
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }

        // Countdown: 5, 4, 3, 2, 1
        for (int i = 1; i <= 5; i++) {
            final int countdown = 6 - i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.broadcastMessage("§c§l" + countdown + "...");

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, i * 20L);
        }

        // Nach 5 Sekunden: Kick alle
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            kickAllPlayers(excuse);
        }, 100L);

        plugin.getLogger().info("Kaffeepause gestartet von " + executor.getName());
    }

    /**
     * Kickt alle Spieler mit der Ausrede
     */
    private void kickAllPlayers(String excuse) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer("§6§l☕ KAFFEEPAUSE ☕\n\n§e" + excuse + "\n\n§7Komm später wieder!");
        }

        plugin.getLogger().info("Alle Spieler wurden gekickt (Kaffeepause)");
    }
}