package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Command: /fuenfminutengodmode
 *
 * Gibt dem ausführenden Spieler für genau 5 Minuten OP-Rechte
 * und entfernt sie danach automatisch wieder.
 *
 * Ein Scherz-Command - aber er funktioniert wirklich! 😄
 */
public class FuenfMinutenGodmodeCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    // Speichert, welche Spieler bereits Godmode haben (verhindert mehrfache Nutzung)
    private final Map<UUID, Integer> activeGodmodes = new HashMap<>();

    public FuenfMinutenGodmodeCommand(AchievementChallengePlugin plugin) {
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
        UUID playerId = player.getUniqueId();

        // Prüfe ob Spieler bereits Godmode aktiv hat
        if (activeGodmodes.containsKey(playerId)) {
            player.sendMessage("§cDu hast bereits einen aktiven Godmode!");
            player.sendMessage("§7Warte bis er abläuft, du kleiner Cheater! 😏");
            return true;
        }

        // Prüfe ob Spieler bereits OP ist
        boolean wasOp = player.isOp();

        if (wasOp) {
            player.sendMessage("§cDu bist bereits OP, du brauchst keinen Godmode!");
            player.sendMessage("§7Netter Versuch! 😄");
            return true;
        }

        // AKTIVIERE GODMODE! 🔥
        player.setOp(true);

        // Broadcast an alle (für maximalen Drama-Effekt)
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§l⚠ ACHTUNG ⚠");
        Bukkit.broadcastMessage("§e" + player.getName() + " §7hat den §6§l5-Minuten-Godmode §7aktiviert!");
        Bukkit.broadcastMessage("§7Er hat jetzt OP-Rechte für §c5 Minuten§7!");
        Bukkit.broadcastMessage("§8(Ich hoffe, du missbrauchst das nicht...)");
        Bukkit.broadcastMessage("");

        // Sound-Effekt für Drama
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }

        // Spezielle Nachricht für den Spieler
        player.sendMessage("§a§l✓ Godmode aktiviert!");
        player.sendMessage("§7Du hast jetzt §c5 Minuten §7OP-Rechte!");
        player.sendMessage("§8Nutze sie weise... oder auch nicht. 😈");

        // Starte Timer (5 Minuten = 6000 Ticks)
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeGodmode(player, playerId, wasOp);
        }, 6000L).getTaskId();

        // Speichere Task-ID
        activeGodmodes.put(playerId, taskId);

        // Warnungen bei 4, 3, 2, 1 Minute verbleibend
        scheduleWarning(player, 1, 1200L);  // Nach 1 Minute (4 Minuten verbleibend)
        scheduleWarning(player, 2, 2400L);  // Nach 2 Minuten (3 Minuten verbleibend)
        scheduleWarning(player, 3, 3600L);  // Nach 3 Minuten (2 Minuten verbleibend)
        scheduleWarning(player, 4, 4800L);  // Nach 4 Minuten (1 Minute verbleibend)
        scheduleWarning(player, 5, 5400L);  // Nach 4:30 Minuten (30 Sekunden verbleibend)

        return true;
    }

    /**
     * Sendet eine Warnung an den Spieler
     *
     * @param player Der Spieler
     * @param minute Welche Minute vorbei ist
     * @param delay Delay in Ticks
     */
    private void scheduleWarning(Player player, int minute, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            int remaining = 5 - minute;

            if (minute == 5) {
                // 30 Sekunden verbleibend
                player.sendMessage("§c§l⚠ Nur noch 30 Sekunden Godmode!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            } else {
                // Minuten-Warnung
                String minuteText = remaining == 1 ? "Minute" : "Minuten";
                player.sendMessage("§e⚠ Noch §c" + remaining + " " + minuteText + " §eGodmode verbleibend!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        }, delay);
    }

    /**
     * Entfernt den Godmode von einem Spieler
     *
     * @param player Der Spieler
     * @param playerId UUID des Spielers
     * @param wasOp War der Spieler vorher schon OP?
     */
    private void removeGodmode(Player player, UUID playerId, boolean wasOp) {
        // Entferne aus aktiven Godmodes
        activeGodmodes.remove(playerId);

        // Prüfe ob Spieler noch online ist
        if (!player.isOnline()) {
            plugin.getLogger().info("Godmode von " + player.getName() + " ist abgelaufen (offline)");
            return;
        }

        // Entferne OP (nur wenn er vorher nicht OP war)
        if (!wasOp) {
            player.setOp(false);
        }

        // Dramatische Beendigung
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§lZEIT IST UM!");
        Bukkit.broadcastMessage("§e" + player.getName() + "§7's Godmode ist §cabgelaufen§7!");
        Bukkit.broadcastMessage("§7Zurück zur Normalität!");
        Bukkit.broadcastMessage("");

        // Sound-Effekt
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 1.5f);
        }

        // Nachricht an den Spieler
        player.sendMessage("§c§l✗ Godmode beendet!");
        player.sendMessage("§7Deine 5 Minuten sind um!");
        player.sendMessage("§7Ich hoffe, du hattest Spaß!");

        plugin.getLogger().info(player.getName() + "'s 5-Minuten-Godmode ist abgelaufen");
    }

    /**
     * Gibt die Map der aktiven Godmodes zurück (für eventuelle Cleanup-Tasks)
     */
    public Map<UUID, Integer> getActiveGodmodes() {
        return activeGodmodes;
    }
}