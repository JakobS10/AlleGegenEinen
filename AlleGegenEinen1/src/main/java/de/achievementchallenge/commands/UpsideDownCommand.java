package de.achievementchallenge.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command: /upsidedown <Spieler>
 *
 * Dreht die Welt für den Spieler für 5 Sekunden auf den Kopf.
 * Manipuliert Kamera-Rotation via ProtocolLib.
 *
 * Nur für Dämonen verfügbar.
 */
public class UpsideDownCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private final Map<UUID, PacketAdapter> activeEffects = new HashMap<>();
    private final Map<UUID, Integer> deactivationTasks = new HashMap<>();

    public UpsideDownCommand(AchievementChallengePlugin plugin) {
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
            player.sendMessage("§7Verwendung: /upsidedown <Spieler>");
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
            player.sendMessage("§c" + target.getName() + " steht bereits Kopf!");
            return true;
        }

        // Aktiviere Upside-Down
        activateUpsideDown(target, player);

        return true;
    }

    /**
     * Aktiviert Upside-Down für 5 Sekunden
     */
    private void activateUpsideDown(Player target, Player executor) {
        String targetName = target.getName();

        // Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("upsidedown");
        announcement = announcement.replace("{target}", targetName);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.5f);
        }

        // Nachricht für Opfer
        target.sendMessage("§c§lDIE WELT STEHT KOPF!");
        target.sendMessage("§7Alles dreht sich...");
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 0.5f);

        // Bestätigung für Dämon
        executor.sendMessage("§a✓ " + targetName + " steht jetzt für 5 Sekunden Kopf!");

        // Erstelle Packet-Listener für Kamera-Rotation
        PacketAdapter adapter = new PacketAdapter(
                plugin,
                PacketType.Play.Server.POSITION,
                PacketType.Play.Server.ENTITY_TELEPORT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                // Nur für den Zielspieler
                if (!event.getPlayer().equals(target)) {
                    return;
                }

                PacketContainer packet = event.getPacket();

                try {
                    // Manipuliere Pitch (Kamera-Neigung)
                    // Normaler Pitch: -90 (oben) bis 90 (unten)
                    // Wir invertieren: pitch = -pitch + 180

                    if (packet.getType() == PacketType.Play.Server.POSITION) {
                        // Player Position Packet
                        float currentPitch = packet.getFloat().read(1); // Pitch
                        float newPitch = -currentPitch; // Invertiere Pitch

                        packet.getFloat().write(1, newPitch);
                    }

                } catch (Exception e) {
                    // Fehler ignorieren
                }
            }
        };

        // Registriere Listener
        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
        activeEffects.put(target.getUniqueId(), adapter);

        // Teleportiere Spieler minimal um Rotation zu aktualisieren
        Bukkit.getScheduler().runTask(plugin, () -> {
            target.teleport(target.getLocation().add(0, 0.1, 0));
        });

        // Deaktiviere nach 5 Sekunden (100 Ticks)
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            deactivateUpsideDown(target);
        }, 100L).getTaskId();

        deactivationTasks.put(target.getUniqueId(), taskId);

        plugin.getLogger().info("Upside-Down aktiviert für " + targetName + " (5 Sekunden)");
    }

    /**
     * Deaktiviert Upside-Down
     */
    private void deactivateUpsideDown(Player target) {
        PacketAdapter adapter = activeEffects.remove(target.getUniqueId());
        Integer taskId = deactivationTasks.remove(target.getUniqueId());

        if (adapter != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
        }

        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        if (target.isOnline()) {
            // Teleportiere zurück zur normalen Rotation
            target.teleport(target.getLocation());

            target.sendMessage("§a§lDie Welt ist wieder normal!");
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        plugin.getLogger().info("Upside-Down deaktiviert für " + target.getName());
    }

    /**
     * Stoppt alle aktiven Effekte (für Server-Shutdown)
     */
    public void stopAll() {
        for (UUID uuid : new HashSet<>(activeEffects.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                deactivateUpsideDown(p);
            }
        }

        activeEffects.clear();
        deactivationTasks.clear();
    }
}