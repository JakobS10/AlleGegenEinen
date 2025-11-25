package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Command: /immun <Spieler>
 *
 * Macht einen Spieler "immun" gegen Bans.
 * Der Spieler wird automatisch alle 20 Sekunden entbannt falls er gebannt wurde.
 * Läuft bis zum Server-Neustart.
 *
 * Nur für Dämonen verfügbar.
 */
public class ImmunCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    // Set aller immunen Spieler (UUID)
    private static final Set<UUID> immunePlayers = new HashSet<>();

    // Task-ID für Auto-Unban-Check
    private static int checkTaskId = -1;

    public ImmunCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        startAutoUnbanCheck();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        if (args.length != 1) {
            player.sendMessage("§cFalsche Verwendung!");
            player.sendMessage("§7Verwendung: /immun <Spieler>");
            return true;
        }

        // Hole Zielspieler (kann auch offline sein)
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            // Versuche offline-Spieler zu finden
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(args[0]);

            if (!offlineTarget.hasPlayedBefore()) {
                player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
                return true;
            }

            // Offline-Spieler immun machen
            makeImmune(offlineTarget.getUniqueId(), offlineTarget.getName(), player);

        } else {
            // Online-Spieler immun machen
            makeImmune(target.getUniqueId(), target.getName(), player);
        }

        return true;
    }

    /**
     * Macht einen Spieler immun
     */
    private void makeImmune(UUID targetUUID, String targetName, Player executor) {
        // Prüfe ob bereits immun
        if (immunePlayers.contains(targetUUID)) {
            executor.sendMessage("§c" + targetName + " ist bereits immun!");
            return;
        }

        // Füge zu immunen Spielern hinzu
        immunePlayers.add(targetUUID);

        // Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("immun");
        announcement = announcement.replace("{target}", targetName);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§6§l⚡ IMMUNITÄT GEWÄHRT ⚡");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        }

        // Bestätigung
        executor.sendMessage("§a✓ " + targetName + " ist jetzt immun gegen Bans!");
        executor.sendMessage("§7Läuft bis zum Server-Neustart.");

        plugin.getLogger().info(targetName + " wurde immun gemacht (von " + executor.getName() + ")");
    }

    /**
     * Startet den Auto-Unban-Check (alle 20 Sekunden)
     */
    private void startAutoUnbanCheck() {
        if (checkTaskId != -1) {
            return; // Läuft bereits
        }

        checkTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkAndUnbanImmunePlayers();
        }, 400L, 400L).getTaskId(); // Alle 20 Sekunden (400 Ticks)

        plugin.getLogger().info("Auto-Unban-Check gestartet (alle 20 Sekunden)");
    }

    /**
     * Prüft ob immune Spieler gebannt sind und entbannt sie
     */
    private void checkAndUnbanImmunePlayers() {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);

        for (UUID immuneUUID : immunePlayers) {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(immuneUUID);
            String playerName = offlinePlayer.getName();

            // Prüfe ob gebannt
            if (playerName != null && banList.isBanned(playerName)) {
                // ENTBANNE!
                banList.pardon(playerName);

                // Lade zufällige Unban-Nachricht
                String message = plugin.getAnnouncementManager().getRandomAnnouncement("immun_unban");
                message = message.replace("{target}", playerName);

                // Broadcast
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§6§l⚡ IMMUNITÄT AKTIVIERT ⚡");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§7" + message);
                Bukkit.broadcastMessage("");

                // Sound
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.2f);
                }

                plugin.getLogger().info(playerName + " wurde automatisch entbannt (Immunität) (Gnihihihi! Ihr könnt mir gar nix)");
            }
        }
    }

    /**
     * Gibt alle immunen Spieler zurück
     */
    public static Set<UUID> getImmunePlayers() {
        return new HashSet<>(immunePlayers);
    }

    /**
     * Stoppt den Auto-Unban-Check (für Server-Shutdown)
     */
    public void stop() {
        if (checkTaskId != -1) {
            Bukkit.getScheduler().cancelTask(checkTaskId);
            checkTaskId = -1;
        }
        immunePlayers.clear();
    }
}