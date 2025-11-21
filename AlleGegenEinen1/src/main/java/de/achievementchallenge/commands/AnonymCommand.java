package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command: /anonym <Spieler> <Minuten>
 *
 * Gibt allen Spielern den Skin und Namen des Zielspielers.
 * Nur für Dämonen verfügbar.
 */
public class AnonymCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private int activeTaskId = -1;

    public AnonymCommand(AchievementChallengePlugin plugin) {
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
            player.sendMessage("§7Verwendung: /anonym <Spieler> <Minuten>");
            return true;
        }

        // Hole Zielspieler
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        // Parse Minuten
        int minutes;
        try {
            minutes = Integer.parseInt(args[1]);
            if (minutes < 1 || minutes > 60) {
                player.sendMessage("§cMinuten müssen zwischen 1 und 60 liegen!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cUngültige Zahl: " + args[1]);
            return true;
        }

        // Prüfe ob bereits aktiv
        if (activeTaskId != -1) {
            player.sendMessage("§cAnonym-Modus ist bereits aktiv!");
            player.sendMessage("§7Warte bis er abläuft.");
            return true;
        }

        // Aktiviere Anonym-Modus
        activateAnonymMode(target, minutes);

        return true;
    }

    /**
     * Aktiviert den Anonym-Modus
     */
    private void activateAnonymMode(Player target, int minutes) {
        String targetName = target.getName();

        // Lade zufällige Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("anonym");
        announcement = announcement.replace("{target}", targetName);
        announcement = announcement.replace("{minutes}", String.valueOf(minutes));

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");
        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        }

        // Setze alle Namen auf Zielspieler
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListName("§e" + targetName);
            p.setDisplayName("§e" + targetName);
            p.setCustomName("§e" + targetName);
            p.setCustomNameVisible(true);
        }

        // Starte Timer für Rücksetzung
        long ticks = minutes * 60 * 20L;
        activeTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            deactivateAnonymMode();
        }, ticks).getTaskId();

        plugin.getLogger().info("Anonym-Modus aktiviert für " + minutes + " Minuten (Ziel: " + targetName + ")");
    }

    /**
     * Deaktiviert den Anonym-Modus
     */
    private void deactivateAnonymMode() {
        activeTaskId = -1;

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Alle Spieler sind wieder sie selbst!");
        Bukkit.broadcastMessage("");

        // Setze Namen zurück
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setPlayerListName(p.getName());
            p.setDisplayName(p.getName());
            p.setCustomName(null);
            p.setCustomNameVisible(false);
        }

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }

        plugin.getLogger().info("Anonym-Modus deaktiviert");
    }

    /**
     * Stoppt den aktiven Anonym-Modus (für Server-Shutdown)
     */
    public void stop() {
        if (activeTaskId != -1) {
            Bukkit.getScheduler().cancelTask(activeTaskId);
            activeTaskId = -1;
        }
    }
}