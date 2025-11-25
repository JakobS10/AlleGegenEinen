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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Command: /psychischbedingt <Spieler>
 *
 * Gibt dem Spieler für 5 Minuten zufällige Fake-Damage-Effekte:
 * - Damage-Sound ohne echten Schaden
 * - Kurzes Rotes Blinken (Wither-Effekt)
 * - Keine Knockback, kein echter Schaden
 *
 * Nur für Dämonen verfügbar.
 */
public class PsychischbedingtCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private final Map<UUID, Integer> activeEffects = new HashMap<>();
    private final Random random = new Random();

    // Verschiedene Damage-Sounds
    private static final Sound[] DAMAGE_SOUNDS = {
            Sound.ENTITY_PLAYER_HURT,
            Sound.ENTITY_PLAYER_HURT_DROWN,
            Sound.ENTITY_PLAYER_HURT_ON_FIRE,
            Sound.ENTITY_PLAYER_HURT_FREEZE
    };

    public PsychischbedingtCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
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
            player.sendMessage("§7Verwendung: /psychischbedingt <Spieler>");
            return true;
        }

        // Hole Zielspieler
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        // Prüfe ob bereits aktiv
        if (activeEffects.containsKey(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " leidet bereits psychisch!");
            return true;
        }

        // Aktiviere Fake-Damage
        activateFakeDamage(target, player);

        return true;
    }

    /**
     * Aktiviert Fake-Damage für 5 Minuten
     */
    private void activateFakeDamage(Player target, Player executor) {
        String targetName = target.getName();

        // Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("psychischbedingt");
        announcement = announcement.replace("{target}", targetName);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 0.5f, 0.8f);
        }

        // Nachricht für Opfer
        target.sendMessage("§c§lDu fühlst dich plötzlich sehr unwohl...");
        target.sendMessage("§7Irgendetwas stimmt nicht...");

        // Bestätigung für Dämon
        executor.sendMessage("§a✓ " + targetName + " leidet jetzt für 5 Minuten psychisch!");

        // Starte Fake-Damage-Loop (5 Minuten = 6000 Ticks)
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticksElapsed = 0;
            final int maxTicks = 6000; // 5 Minuten
            int nextDamageIn = getRandomInterval(); // Zufälliges erstes Intervall

            @Override
            public void run() {
                ticksElapsed++;

                // Nach 5 Minuten beenden
                if (ticksElapsed >= maxTicks || !target.isOnline()) {
                    deactivateFakeDamage(target);
                    return;
                }

                // Countdown zum nächsten Fake-Damage
                nextDamageIn--;

                if (nextDamageIn <= 0) {
                    // FAKE DAMAGE!
                    applyFakeDamage(target);

                    // Nächstes zufälliges Intervall (3-15 Sekunden)
                    nextDamageIn = getRandomInterval();
                }
            }
        }, 0L, 1L).getTaskId(); // Jeder Tick

        activeEffects.put(target.getUniqueId(), taskId);

        plugin.getLogger().info("Fake-Damage aktiviert für " + targetName + " (5 Minuten)");
    }

    /**
     * Wendet einen Fake-Damage-Effekt an
     */
    private void applyFakeDamage(Player target) {
        // 1. Gebe kurzzeitig sehr hohe Resistance damit kein echter Schaden entsteht
        PotionEffect resistance = new PotionEffect(
                PotionEffectType.RESISTANCE,
                20, // 1 Sekunde
                255, // Sehr hoch = Unverwundbar
                false,
                false,
                false
        );
        target.addPotionEffect(resistance);

        // 2. Füge kleinen echten Schaden zu (wird durch Resistance geblockt, aber Animation wird angezeigt!)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.damage(0.1); // Minimaler Schaden = nur Animation
            }
        }, 2L);

        // 3. Zufälliger Damage-Sound für den Spieler selbst
        Sound damageSound = DAMAGE_SOUNDS[random.nextInt(DAMAGE_SOUNDS.length)];
        target.playSound(target.getLocation(), damageSound, 1.0f, 1.0f);

        // 4. Rotes Blinken durch kurzen Wither-Effekt
        PotionEffect wither = new PotionEffect(
                PotionEffectType.WITHER,
                10, // 0.5 Sekunden
                0,
                false,
                true, // Partikel zeigen
                false
        );
        target.addPotionEffect(wither);

        // 5. Gelegentlich: Screen Shake (Nausea kurz)
        if (random.nextInt(3) == 0) { // 33% Chance
            PotionEffect nausea = new PotionEffect(
                    PotionEffectType.NAUSEA,
                    20, // 1 Sekunde
                    0,
                    false,
                    false,
                    false
            );
            target.addPotionEffect(nausea);
        }

        // 6. Damage-Tilt-Effekt simulieren (kurzes Slow)
        PotionEffect slowness = new PotionEffect(
                PotionEffectType.SLOWNESS,
                5, // 0.25 Sekunden
                1,
                false,
                false,
                false
        );
        target.addPotionEffect(slowness);
    }

    /**
     * Gibt ein zufälliges Intervall zurück (3-15 Sekunden in Ticks)
     */
    private int getRandomInterval() {
        return (60 + random.nextInt(240)); // 60-300 Ticks = 3-15 Sekunden
    }

    /**
     * Deaktiviert den Fake-Damage-Effekt
     */
    private void deactivateFakeDamage(Player target) {
        Integer taskId = activeEffects.remove(target.getUniqueId());

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (target.isOnline()) {
            target.sendMessage("§a§lDu fühlst dich wieder normal!");
            target.sendMessage("§7Was auch immer das war... es ist vorbei.");
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
        }

        plugin.getLogger().info("Fake-Damage deaktiviert für " + target.getName());
    }

    /**
     * Stoppt alle aktiven Effekte (für Server-Shutdown)
     */
    public void stopAll() {
        for (Integer taskId : activeEffects.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        activeEffects.clear();
    }
}