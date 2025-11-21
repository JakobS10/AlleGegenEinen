package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Command: /lag
 *
 * Friert alle Spieler, Mobs und Entities für genau 10 Sekunden ein.
 * Nur für Dämonen verfügbar.
 */
public class LagCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private boolean lagActive = false;
    private int lagTaskId = -1;

    // Speichert Original-Gamemodes
    private final Map<UUID, GameMode> originalGameModes = new HashMap<>();

    // Speichert Original-Positionen für Entities
    private final Map<UUID, Location> originalLocations = new HashMap<>();

    public LagCommand(AchievementChallengePlugin plugin) {
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

        // Prüfe ob bereits aktiv
        if (lagActive) {
            player.sendMessage("§cLag ist bereits aktiv!");
            player.sendMessage("§7Warte bis die 10 Sekunden vorbei sind.");
            return true;
        }

        // Aktiviere Lag für 10 Sekunden
        activateLag();

        return true;
    }

    /**
     * Aktiviert den Lag-Modus für 10 Sekunden
     */
    private void activateLag() {
        lagActive = true;

        // Lade zufällige Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("lag");

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Warte bitte kurz §c10 Sekunden");
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }

        // Friere alle Spieler ein
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Speichere Original-Gamemode
            originalGameModes.put(p.getUniqueId(), p.getGameMode());

            // Setze auf Adventure (verhindert Blocken/Brechen)
            p.setGameMode(GameMode.ADVENTURE);

            // Gebe Slowness und Jump Boost Negativeffekt
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 255, false, false)); // 10 Sekunden
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 200, 250, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 255, false, false));

            // Nachricht
            p.sendMessage("§c§lDu bist eingefroren!");
            p.sendMessage("§7Warte 10 Sekunden...");
        }

        // Friere alle Entities ein
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    LivingEntity living = (LivingEntity) entity;

                    // Speichere Position
                    originalLocations.put(entity.getUniqueId(), entity.getLocation().clone());

                    // Setze AI aus
                    living.setAI(false);

                    // Gebe Effekte
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 255, false, false));
                }
            }
        }

        // Plane automatische Deaktivierung nach 10 Sekunden (200 Ticks)
        lagTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            deactivateLag();
        }, 200L).getTaskId();

        plugin.getLogger().info("Lag-Modus aktiviert für 10 Sekunden");
    }

    /**
     * Deaktiviert den Lag-Modus
     */
    private void deactivateLag() {
        lagActive = false;
        lagTaskId = -1;

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§a§l✓ LAG BEHOBEN");
        Bukkit.broadcastMessage("§7Die Zeit läuft wieder normal!");
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        }

        // Entfriere alle Spieler
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Stelle Original-Gamemode wieder her
            GameMode original = originalGameModes.get(p.getUniqueId());
            if (original != null) {
                p.setGameMode(original);
            }

            // Entferne Effekte
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            p.removePotionEffect(PotionEffectType.JUMP_BOOST);
            p.removePotionEffect(PotionEffectType.MINING_FATIGUE);

            // Nachricht
            p.sendMessage("§a§lDu bist frei!");
        }

        originalGameModes.clear();

        // Entfriere alle Entities
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    LivingEntity living = (LivingEntity) entity;

                    // Aktiviere AI wieder
                    living.setAI(true);

                    // Entferne Effekte
                    living.removePotionEffect(PotionEffectType.SLOWNESS);
                }
            }
        }

        originalLocations.clear();

        plugin.getLogger().info("Lag-Modus automatisch beendet nach 10 Sekunden");
    }

    /**
     * Gibt an ob Lag aktiv ist
     */
    public boolean isLagActive() {
        return lagActive;
    }

    /**
     * Stoppt Lag (für Server-Shutdown)
     */
    public void stop() {
        if (lagActive) {
            if (lagTaskId != -1) {
                Bukkit.getScheduler().cancelTask(lagTaskId);
            }
            deactivateLag();
        }
    }
}