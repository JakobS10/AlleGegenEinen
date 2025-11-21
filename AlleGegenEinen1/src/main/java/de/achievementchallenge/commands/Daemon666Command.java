package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.DaemonManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /666
 *
 * Macht den Sender zum "Nervigen Dämon" und gibt ihm spezielle Troll-Rechte.
 * Wird mit dramatischem Effekt für alle Spieler angekündigt.
 */
public class Daemon666Command implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public Daemon666Command(AchievementChallengePlugin plugin) {
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
        DaemonManager dm = plugin.getDaemonManager();

        // Prüfe ob Spieler bereits Dämon ist
        if (dm.isDaemon(player.getUniqueId())) {
            player.sendMessage("§cDu bist bereits ein Dämon!");
            player.sendMessage("§7Nutze deine dunklen Kräfte weise... 😈");
            return true;
        }

        // Mache zum Dämon
        dm.addDaemon(player.getUniqueId(), player.getName());

        // Lade zufällige Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("daemon_summon");

        // Ersetze Platzhalter
        announcement = announcement.replace("{player}", player.getName());

        // Dramatischer Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§4§l§k|||§r §c§l⚠ DÄMON BESCHWOREN ⚠ §4§l§k|||");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c" + announcement);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Er hat nun Zugriff auf die dunklen Künste...");
        Bukkit.broadcastMessage("§8Möge Gott uns allen gnädig sein.");
        Bukkit.broadcastMessage("");

        // Dramatische Sound-Effekte für alle
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

            // Extra Sound für den Dämon selbst
            if (p.equals(player)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
                }, 20L);
            }
        }

        // Spezielle Nachricht für den Dämon
        player.sendMessage("");
        player.sendMessage("§4§l✦ Du bist nun ein Dämon! ✦");
        player.sendMessage("");
        player.sendMessage("§7Deine dunklen Fähigkeiten:");
        player.sendMessage("§c/anonym §7- Alle werden zu einer Person");
        player.sendMessage("§c/audiotest §7- Quäle jemanden mit Sounds");
        player.sendMessage("§c/kaffepause §7- Kick alle mit Ausrede");
        player.sendMessage("§c/lag §7- Friere alle ein");
        player.sendMessage("");
        player.sendMessage("§8Nutze /befreitmich um deine Macht abzulegen...");
        player.sendMessage("");

        // Speichere Änderungen
        plugin.getDataManager().saveData();

        return true;
    }
}