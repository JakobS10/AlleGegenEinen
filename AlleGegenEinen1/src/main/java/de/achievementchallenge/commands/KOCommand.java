package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

/**
 * Command: /ko
 *
 * Kickt alle Spieler, die gerade den Blur-Effekt (Darkness/Blindness) haben.
 * Perfekt um geblurete Spieler "fertig zu machen".
 *
 * Nur für Dämonen verfügbar.
 */
public class KOCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public KOCommand(AchievementChallengePlugin plugin) {
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

        // Finde alle geblurete Spieler
        int kickedCount = 0;

        for (Player target : Bukkit.getOnlinePlayers()) {
            // Prüfe ob Spieler Darkness oder Blindness hat
            boolean hasDarkness = target.hasPotionEffect(PotionEffectType.DARKNESS);
            boolean hasBlindness = target.hasPotionEffect(PotionEffectType.BLINDNESS);

            if (hasDarkness || hasBlindness) {
                kickBlurredPlayer(target);
                kickedCount++;
            }
        }

        // Ergebnis
        if (kickedCount == 0) {
            player.sendMessage("§cKeine geblurete Spieler gefunden!");
            player.sendMessage("§7Nutze erst /blur um jemanden zu blenden.");
        } else {
            player.sendMessage("§a✓ " + kickedCount + " geblurte Spieler wurden gekickt!");
            plugin.getLogger().info(kickedCount + " geblurte Spieler wurden gekickt (von " + player.getName() + ")");
        }

        return true;
    }

    /**
     * Kickt einen geblurete Spieler mit dramatischer Nachricht
     */
    private void kickBlurredPlayer(Player target) {
        String targetName = target.getName();

        // Lade zufällige Ankündigung für alle
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("ko");
        announcement = announcement.replace("{target}", targetName);

        // Broadcast BEVOR der Kick erfolgt
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§l⚡ K.O. ⚡");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        // Sound für alle (außer das Opfer, der hört es eh nicht mehr)
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(target)) {
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.0f);
            }
        }

        // Lade zufällige Kick-Nachricht
        String kickMessage = plugin.getAnnouncementManager().getRandomAnnouncement("ko_kick");

        // Kleine Verzögerung damit der Sound/Broadcast noch ankommt
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            target.kickPlayer("§c§l⚡ K.O. ⚡\n\n" +
                    "§7" + kickMessage + "\n\n" +
                    "§8Du warst geblendet und wurdest fertig gemacht...");
        }, 20L); // 1 Sekunde Verzögerung
    }
}