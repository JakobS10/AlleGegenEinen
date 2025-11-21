package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /ankuendigung <Nachricht>
 *
 * Sendet eine Nachricht in rot und fett ohne Benutzernamen in den Chat.
 * Nur für Dämonen verfügbar.
 */
public class AnkuendigungCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public AnkuendigungCommand(AchievementChallengePlugin plugin) {
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

        // Prüfe Argumente
        if (args.length == 0) {
            player.sendMessage("§cFalsche Verwendung!");
            player.sendMessage("§7Verwendung: /ankuendigung <Nachricht>");
            return true;
        }

        // Baue Nachricht zusammen
        StringBuilder message = new StringBuilder();
        for (String arg : args) {
            message.append(arg).append(" ");
        }

        // Sende Ankündigung (rot und fett)
        String announcement = "§c§l" + message.toString().trim();

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(announcement);
        Bukkit.broadcastMessage("");

        // Bestätigung für Dämon
        player.sendMessage("§a✓ Ankündigung gesendet!");

        plugin.getLogger().info("Ankündigung von " + player.getName() + ": " + message.toString().trim());

        return true;
    }
}