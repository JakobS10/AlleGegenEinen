package de.achievementchallenge.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command: /schatten <Spieler>
 *
 * Toggle-Command: Spawnt einen Doppelgänger der alle Bewegungen 2 Sekunden verzögert nachahmt.
 * Der Schatten ist für alle sichtbar!
 * Nochmaliges Ausführen entfernt den Schatten.
 *
 * Nur für Dämonen verfügbar.
 */
public class SchattenCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private final ProtocolManager protocolManager;

    // Speichert welche Spieler einen Schatten haben
    private final Map<UUID, ShadowData> activeShadows = new HashMap<>();

    public SchattenCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
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
            player.sendMessage("§7Verwendung: /schatten <Spieler>");
            return true;
        }

        // Hole Zielspieler
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        // Toggle: Aktivieren oder Deaktivieren
        if (activeShadows.containsKey(target.getUniqueId())) {
            deactivateShadow(target, player);
        } else {
            activateShadow(target, player);
        }

        return true;
    }

    /**
     * Aktiviert den Schatten
     */
    private void activateShadow(Player target, Player executor) {
        String targetName = target.getName();

        // Ankündigung
        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("schatten");
        announcement = announcement.replace("{target}", targetName);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }

        // Nachricht für Opfer
        target.sendMessage("§8§lEin Schatten folgt dir...");
        target.sendMessage("§7Er kopiert jede deiner Bewegungen...");

        // Bestätigung für Dämon
        executor.sendMessage("§a✓ Schatten aktiviert für " + targetName + "!");

        // Erstelle Schatten-Daten
        int shadowEntityId = new Random().nextInt(100000) + 100000; // Zufällige Entity-ID
        ShadowData shadowData = new ShadowData(target, shadowEntityId);
        activeShadows.put(target.getUniqueId(), shadowData);

        // Spawne Schatten für alle Spieler
        spawnShadowForAll(shadowData);

        // Starte Movement-Tracker
        startMovementTracking(shadowData);

        plugin.getLogger().info("Schatten aktiviert für " + targetName);
    }

    /**
     * Spawnt den Schatten-Entity für alle Spieler
     */
    private void spawnShadowForAll(ShadowData shadowData) {
        Player target = shadowData.target;

        try {
            // Hole Original-Profil
            WrappedGameProfile targetProfile = WrappedGameProfile.fromPlayer(target);

            // Erstelle Schatten-Profil mit neuer UUID aber gleichem Namen
            WrappedGameProfile shadowProfile = new WrappedGameProfile(
                    UUID.randomUUID(), // Neue UUID für Schatten
                    target.getName() // Gleicher Name (Client zeigt ihn dunkel wegen anderem Entity)
            );

            // Kopiere Skin-Properties (sicherer Weg)
            try {
                for (String key : targetProfile.getProperties().keySet()) {
                    shadowProfile.getProperties().putAll(key, targetProfile.getProperties().get(key));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Konnte Skin-Properties nicht kopieren, verwende Standard-Skin");
            }

            // Sende PlayerInfo ADD für Schatten
            PacketContainer infoPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

            PlayerInfoData shadowInfo = new PlayerInfoData(
                    shadowProfile,
                    0, // Ping
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    WrappedChatComponent.fromText("§8Schatten")
            );

            infoPacket.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
            infoPacket.getPlayerInfoDataLists().write(1, Arrays.asList(shadowInfo));

            // Sende Spawn-Packet
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, shadowData.entityId); // Entity ID
            spawnPacket.getUUIDs().write(0, shadowProfile.getUUID()); // UUID

            Location loc = target.getLocation();
            spawnPacket.getDoubles().write(0, loc.getX());
            spawnPacket.getDoubles().write(1, loc.getY());
            spawnPacket.getDoubles().write(2, loc.getZ());

            spawnPacket.getBytes().write(0, (byte) ((int) (loc.getYaw() * 256.0F / 360.0F)));
            spawnPacket.getBytes().write(1, (byte) ((int) (loc.getPitch() * 256.0F / 360.0F)));

            // Sende an alle Spieler
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(viewer, infoPacket);
                protocolManager.sendServerPacket(viewer, spawnPacket);
            }

            shadowData.shadowProfile = shadowProfile;

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Spawnen des Schattens: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Startet das Movement-Tracking
     */
    private void startMovementTracking(ShadowData shadowData) {
        // Task der alle 2 Ticks (0.1 Sekunden) die Position aufzeichnet
        int recordTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!shadowData.target.isOnline()) {
                deactivateShadow(shadowData.target, null);
                return;
            }

            // Speichere aktuelle Position mit Timestamp
            Location currentLoc = shadowData.target.getLocation().clone();
            shadowData.locationHistory.add(new LocationSnapshot(currentLoc, System.currentTimeMillis()));

            // Halte nur die letzten 2.5 Sekunden (für 2 Sekunden Verzögerung + Puffer)
            while (!shadowData.locationHistory.isEmpty()) {
                LocationSnapshot oldest = shadowData.locationHistory.peek();
                if (System.currentTimeMillis() - oldest.timestamp > 2500) {
                    shadowData.locationHistory.poll();
                } else {
                    break;
                }
            }

        }, 0L, 2L).getTaskId(); // Alle 2 Ticks

        // Task der alle 2 Ticks den Schatten zur 2-Sekunden-alten Position bewegt
        int updateTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!shadowData.target.isOnline()) {
                return;
            }

            // Finde Position von vor 2 Sekunden
            long targetTime = System.currentTimeMillis() - 2000; // 2 Sekunden zurück
            LocationSnapshot targetSnapshot = null;

            for (LocationSnapshot snapshot : shadowData.locationHistory) {
                if (snapshot.timestamp <= targetTime) {
                    targetSnapshot = snapshot;
                } else {
                    break;
                }
            }

            if (targetSnapshot != null) {
                // Bewege Schatten zu dieser Position
                updateShadowPosition(shadowData, targetSnapshot.location);
            }

        }, 40L, 2L).getTaskId(); // Start nach 2 Sekunden, dann alle 2 Ticks

        shadowData.recordTaskId = recordTaskId;
        shadowData.updateTaskId = updateTaskId;
    }

    /**
     * Aktualisiert die Position des Schattens für alle Spieler
     */
    private void updateShadowPosition(ShadowData shadowData, Location newLoc) {
        try {
            // Sende Entity Teleport Packet
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            // Setze Entity ID
            teleportPacket.getIntegers().write(0, shadowData.entityId);

            // Setze Position (Doubles)
            teleportPacket.getDoubles().write(0, newLoc.getX());
            teleportPacket.getDoubles().write(1, newLoc.getY());
            teleportPacket.getDoubles().write(2, newLoc.getZ());

            // Setze Rotation (Bytes) - Umrechnung: Grad * 256 / 360
            teleportPacket.getBytes().write(0, (byte) ((int) (newLoc.getYaw() * 256.0F / 360.0F)));
            teleportPacket.getBytes().write(1, (byte) ((int) (newLoc.getPitch() * 256.0F / 360.0F)));

            // On Ground
            teleportPacket.getBooleans().write(0, true);

            // Sende an alle Spieler
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                try {
                    protocolManager.sendServerPacket(viewer, teleportPacket);
                } catch (Exception e) {
                    // Spieler möglicherweise offline, ignorieren
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Updaten der Schatten-Position: " + e.getMessage());
        }
    }

    /**
     * Deaktiviert den Schatten
     */
    private void deactivateShadow(Player target, Player executor) {
        ShadowData shadowData = activeShadows.remove(target.getUniqueId());

        if (shadowData == null) {
            if (executor != null) {
                executor.sendMessage("§c" + target.getName() + " hat keinen Schatten!");
            }
            return;
        }

        // Stoppe Tasks
        if (shadowData.recordTaskId != -1) {
            Bukkit.getScheduler().cancelTask(shadowData.recordTaskId);
        }
        if (shadowData.updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(shadowData.updateTaskId);
        }

        // Entferne Schatten-Entity für alle Spieler
        try {
            // Entity Destroy Packet
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, Arrays.asList(shadowData.entityId));
            // PlayerInfo REMOVE
            PacketContainer removePacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            removePacket.getModifier().write(0, Arrays.asList(shadowData.shadowProfile.getUUID()));

            // Sende an alle
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(viewer, destroyPacket);
                protocolManager.sendServerPacket(viewer, removePacket);
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Entfernen des Schattens: " + e.getMessage());
        }

        // Nachricht
        if (target.isOnline()) {
            target.sendMessage("§a§lDein Schatten ist verschwunden!");
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        if (executor != null) {
            executor.sendMessage("§a✓ Schatten entfernt von " + target.getName() + "!");
        }

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + target.getName() + "'s Schatten ist verschwunden!");
        Bukkit.broadcastMessage("");

        plugin.getLogger().info("Schatten deaktiviert für " + target.getName());
    }

    /**
     * Stoppt alle aktiven Schatten (für Server-Shutdown)
     */
    public void stopAll() {
        for (UUID uuid : new HashSet<>(activeShadows.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                deactivateShadow(p, null);
            }
        }
        activeShadows.clear();
    }

    /**
     * Daten-Klasse für Schatten
     */
    private static class ShadowData {
        final Player target;
        final int entityId;
        WrappedGameProfile shadowProfile;
        final Queue<LocationSnapshot> locationHistory = new LinkedList<>();
        int recordTaskId = -1;
        int updateTaskId = -1;

        ShadowData(Player target, int entityId) {
            this.target = target;
            this.entityId = entityId;
        }
    }

    /**
     * Location-Snapshot mit Timestamp
     */
    private static class LocationSnapshot {
        final Location location;
        final long timestamp;

        LocationSnapshot(Location location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }
}