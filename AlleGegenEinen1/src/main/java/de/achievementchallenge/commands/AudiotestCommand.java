package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command: /audiotest <Spieler>
 *
 * Spielt 10 Minuten lang in zufälligen Abständen (20-40 Sek) Horror-Sounds beim Zielspieler.
 * Nur für Dämonen verfügbar.
 */
public class AudiotestCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private final Map<UUID, Integer> activeTests = new HashMap<>();
    private final Random random = new Random();

    // Horror-Sounds die abgespielt werden
    private static final Sound[] HORROR_SOUNDS = {
            Sound.ENTITY_CREEPER_PRIMED,
            Sound.ENTITY_ZOMBIE_AMBIENT,
            Sound.ENTITY_ZOMBIE_HURT,
            Sound.ENTITY_SKELETON_AMBIENT,
            Sound.ENTITY_ENDERMAN_SCREAM,
            Sound.ENTITY_GHAST_SCREAM,
            Sound.ENTITY_WITHER_AMBIENT,
            Sound.ENTITY_PHANTOM_AMBIENT
    };

    public AudiotestCommand(AchievementChallengePlugin plugin) {
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
        if (args.length != 1) {
            player.sendMessage("§cFalsche Verwendung!");
            player.sendMessage("§7Verwendung: /audiotest <Spieler>");
            return true;
        }

        // Hole Zielspieler
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        // Prüfe ob bereits aktiv
        if (activeTests.containsKey(target.getUniqueId())) {
            player.sendMessage("§cAudiotest läuft bereits für " + target.getName() + "!");
            return true;
        }

        // Starte Audiotest
        startAudiotest(target);

        return true;
    }

    /**
     * Startet den Audiotest für einen Spieler
     */
    private void startAudiotest(Player target) {
        String targetName = target.getName();

        // Lade zufällige Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("audiotest");
        announcement = announcement.replace("{target}", targetName);

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("§7Dauer: §c10 Minuten");
        Bukkit.broadcastMessage("");

        // Nachricht für Opfer
        target.sendMessage("§c§lOh nein...");
        target.sendMessage("§7Die Monster kommen... ");

        // Starte Sound-Loop
        scheduleNextSound(target, 0);

        plugin.getLogger().info("Audiotest gestartet für " + targetName);
    }

    /**
     * Plant den nächsten Sound ein
     *
     * @param target Zielspieler
     * @param elapsed Vergangene Zeit in Sekunden
     */
    private void scheduleNextSound(Player target, int elapsed) {
        // Nach 10 Minuten (600 Sekunden) beenden
        if (elapsed >= 600) {
            stopAudiotest(target);
            return;
        }

        // Zufälliger Delay zwischen 20 und 40 Sekunden
        int delaySeconds = 20 + random.nextInt(21);
        long delayTicks = delaySeconds * 20L;

        // Plane nächsten Sound
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Prüfe ob Spieler noch online ist
            if (!target.isOnline()) {
                stopAudiotest(target);
                return;
            }

            // Spiele zufälligen Horror-Sound
            Sound sound = HORROR_SOUNDS[random.nextInt(HORROR_SOUNDS.length)];
            float pitch = 0.8f + random.nextFloat() * 0.4f; // Zufällige Tonhöhe 0.8-1.2

            target.playSound(target.getLocation(), sound, 1.0f, pitch);

            // Plane nächsten Sound
            scheduleNextSound(target, elapsed + delaySeconds);

        }, delayTicks).getTaskId();

        activeTests.put(target.getUniqueId(), taskId);
    }

    /**
     * Stoppt den Audiotest
     */
    private void stopAudiotest(Player target) {
        Integer taskId = activeTests.remove(target.getUniqueId());

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (target.isOnline()) {
            target.sendMessage("");
            target.sendMessage("§a§l✓ Audiotest beendet!");
            target.sendMessage("§7Die Monster sind weg... vorerst. ");
            target.sendMessage("");
        }


        plugin.getLogger().info("Audiotest beendet für " + target.getName());
    }

    /**
     * Stoppt alle aktiven Audiotests (für Server-Shutdown)
     */
    public void stopAll() {
        for (Integer taskId : activeTests.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        activeTests.clear();
    }
}