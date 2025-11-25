package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Command: /blur <Spieler> <Sekunden>
 *
 * Macht den Bildschirm des Zielspielers verschwommen durch:
 * - Darkness-Effekt (dunkler Blur-Rand)
 * - Blindness-Effekt (schwarzer Nebel)
 * - Nausea-Effekt (Bildschirm wackelt leicht)
 *
 * Nur für Dämonen verfügbar.
 */
public class BlurCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public BlurCommand(AchievementChallengePlugin plugin) {
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
        if (args.length != 2) {
            player.sendMessage("§cFalsche Verwendung!");
            player.sendMessage("§7Verwendung: /blur <Spieler> <Sekunden>");
            return true;
        }

        // Hole Zielspieler
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        // Parse Sekunden
        int seconds;
        try {
            seconds = Integer.parseInt(args[1]);
            if (seconds < 1 || seconds > 300) { // Max 5 Minuten
                player.sendMessage("§cSekunden müssen zwischen 1 und 300 liegen!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültige Zahl: " + args[1]);
            return true;
        }

        // Wende Blur-Effekte an
        applyBlurEffects(target, seconds, player);

        return true;
    }

    /**
     * Wendet Blur-Effekte auf den Zielspieler an
     */
    private void applyBlurEffects(Player target, int seconds, Player executor) {
        String targetName = target.getName();

        // Lade zufällige Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("blur");
        announcement = announcement.replace("{target}", targetName);
        announcement = announcement.replace("{seconds}", String.valueOf(seconds));

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        // Sound für alle
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_TENDRIL_CLICKS, 0.8f, 0.6f);
        }

        int durationTicks = seconds * 20;

        // Kombiniere mehrere Effekte für maximalen Blur:

        // 1. Darkness (der beste Blur-Effekt seit 1.19)
        PotionEffect darkness = new PotionEffect(
                PotionEffectType.DARKNESS,
                durationTicks,
                0,      // Amplifier
                false,  // Ambient
                true,   // Particles
                true    // Icon
        );
        target.addPotionEffect(darkness);

        // 2. Blindness (schwarzer Nebel)
        PotionEffect blindness = new PotionEffect(
                PotionEffectType.BLINDNESS,
                durationTicks,
                0,
                false,
                true,
                true
        );
        target.addPotionEffect(blindness);

        // 3. Nausea (leichtes Wackeln für zusätzliche Desorientierung)
        PotionEffect nausea = new PotionEffect(
                PotionEffectType.NAUSEA,
                durationTicks,
                0,
                false,
                false,  // Keine Partikel
                false   // Kein Icon (zu viele Icons nerven)
        );
        target.addPotionEffect(nausea);

        // Nachricht für Opfer
        target.sendMessage("§8§lVERSCHWOMMEN");
        target.sendMessage("§7Dauer: §c" + seconds + " Sekunden");
        target.playSound(target.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 0.5f);

        // Bestätigung für Dämon
        executor.sendMessage("§a✓ " + targetName + "'s Bildschirm ist jetzt für " + seconds + " Sekunden verschwommen!");

        plugin.getLogger().info("Blur-Effekte angewendet auf " + targetName + " für " + seconds + " Sekunden (von " + executor.getName() + ")");
    }
}