package de.achievementchallenge.commands;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.*;
import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Command: /anonym <Spieler> <Minuten>
 *
 * Gibt allen Spielern den Skin und Namen des Zielspielers.
 * Nutzt ProtocolLib für echte Skin-Änderung.
 */
public class AnonymCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;
    private final ProtocolManager protocolManager;
    private int activeTaskId = -1;

    // Speichert Original-Profildaten für Wiederherstellung
    private final Map<UUID, WrappedGameProfile> originalProfiles = new HashMap<>();

    public AnonymCommand(AchievementChallengePlugin plugin) {
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
     * Aktiviert den Anonym-Modus mit Skin-Änderung
     */
    private void activateAnonymMode(Player target, int minutes) {
        String targetName = target.getName();

        // Speichere Original-Profile
        for (Player p : Bukkit.getOnlinePlayers()) {
            originalProfiles.put(p.getUniqueId(), WrappedGameProfile.fromPlayer(p));
        }

        // Hole Target-Profil
        WrappedGameProfile targetProfile = WrappedGameProfile.fromPlayer(target);

        // Lade Ankündigung
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

        // Ändere Skins und Namen für alle Spieler
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Setze Display-Namen
            p.setPlayerListName("§e" + targetName);
            p.setDisplayName("§e" + targetName);
            p.setCustomName("§e" + targetName);
            p.setCustomNameVisible(true);

            // Ändere Skin via ProtocolLib
            changeSkinForAll(p, targetProfile);
        }

        // Starte Timer für Rücksetzung
        long ticks = minutes * 60 * 20L;
        activeTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            deactivateAnonymMode();
        }, ticks).getTaskId();

        plugin.getLogger().info("Anonym-Modus aktiviert für " + minutes + " Minuten (Ziel: " + targetName + ")");
    }

    /**
     * Ändert den Skin eines Spielers für alle anderen
     */
    private void changeSkinForAll(Player player, WrappedGameProfile targetProfile) {
        try {
            // Erstelle neues Profil mit Target-Skin aber Original-UUID
            WrappedGameProfile newProfile = new WrappedGameProfile(
                    player.getUniqueId(),
                    player.getName() // Verwende Original-Namen, nicht targetProfile.getName()
            );

            // Kopiere Skin-Properties (sicherer Weg)
            try {
                for (String key : targetProfile.getProperties().keySet()) {
                    newProfile.getProperties().putAll(key, targetProfile.getProperties().get(key));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Konnte Skin-Properties nicht kopieren: " + e.getMessage());
                return; // Abbrechen wenn Skin nicht kopiert werden kann
            }

            // Sende PlayerInfo REMOVE für alle anderen
            PacketContainer removePacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            removePacket.getModifier().write(0, Arrays.asList(player.getUniqueId()));

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) {
                    protocolManager.sendServerPacket(viewer, removePacket);
                }
            }

            // Kurze Verzögerung dann ADD mit neuem Profil
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // Sende PlayerInfo ADD mit neuem Profil
                    PacketContainer addPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

                    // Erstelle PlayerInfoData mit neuem Profil
                    EnumWrappers.PlayerInfoAction action = EnumWrappers.PlayerInfoAction.ADD_PLAYER;
                    PlayerInfoData data = new PlayerInfoData(
                            newProfile,
                            player.getPing(),
                            EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
                            WrappedChatComponent.fromText(player.getDisplayName())
                    );

                    addPacket.getPlayerInfoActions().write(0, EnumSet.of(action));
                    addPacket.getPlayerInfoDataLists().write(1, Arrays.asList(data));

                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.equals(player)) {
                            protocolManager.sendServerPacket(viewer, addPacket);
                        }
                    }

                    // Respawn für visuelles Update
                    respawnPlayerForAll(player);

                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Senden des ADD-Pakets: " + e.getMessage());
                }
            }, 5L);

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Ändern des Skins: " + e.getMessage());
        }
    }

    /**
     * Respawnt den Spieler visuell für alle anderen (ohne Tod)
     */
    private void respawnPlayerForAll(Player player) {
        try {
            // Sende RESPAWN Packet für visuelles Update
            PacketContainer respawnPacket = protocolManager.createPacket(PacketType.Play.Server.RESPAWN);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) {
                    // Player aus Sichtweite entfernen und wieder hinzufügen
                    viewer.hidePlayer(plugin, player);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        viewer.showPlayer(plugin, player);
                    }, 2L);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Respawn: " + e.getMessage());
        }
    }

    /**
     * Deaktiviert den Anonym-Modus und stellt alles wieder her
     */
    private void deactivateAnonymMode() {
        activeTaskId = -1;

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Alle Spieler sind wieder sie selbst!");
        Bukkit.broadcastMessage("");

        // Stelle Namen und Skins wieder her
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Setze Namen zurück
            p.setPlayerListName(p.getName());
            p.setDisplayName(p.getName());
            p.setCustomName(null);
            p.setCustomNameVisible(false);

            // Stelle Original-Skin wieder her
            WrappedGameProfile originalProfile = originalProfiles.get(p.getUniqueId());
            if (originalProfile != null) {
                restoreSkinForAll(p, originalProfile);
            }
        }

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }

        // Cleanup
        originalProfiles.clear();

        plugin.getLogger().info("Anonym-Modus deaktiviert");
    }

    /**
     * Stellt den Original-Skin wieder her
     */
    private void restoreSkinForAll(Player player, WrappedGameProfile originalProfile) {
        try {
            // Sende REMOVE
            PacketContainer removePacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            removePacket.getModifier().write(0, Arrays.asList(player.getUniqueId()));

            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (!viewer.equals(player)) {
                    protocolManager.sendServerPacket(viewer, removePacket);
                }
            }

            // Kurze Verzögerung dann ADD mit Original-Profil
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    PacketContainer addPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

                    PlayerInfoData data = new PlayerInfoData(
                            originalProfile,
                            player.getPing(),
                            EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
                            WrappedChatComponent.fromText(player.getDisplayName())
                    );

                    addPacket.getPlayerInfoActions().write(0, EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER));
                    addPacket.getPlayerInfoDataLists().write(1, Arrays.asList(data));

                    for (Player viewer : Bukkit.getOnlinePlayers()) {
                        if (!viewer.equals(player)) {
                            protocolManager.sendServerPacket(viewer, addPacket);
                        }
                    }

                    // Respawn für visuelles Update
                    respawnPlayerForAll(player);

                } catch (Exception e) {
                    plugin.getLogger().warning("Fehler beim Wiederherstellen: " + e.getMessage());
                }
            }, 5L);

        } catch (Exception e) {
            plugin.getLogger().warning("Fehler beim Wiederherstellen des Skins: " + e.getMessage());
        }
    }

    /**
     * Stoppt den aktiven Anonym-Modus (für Server-Shutdown)
     */
    public void stop() {
        if (activeTaskId != -1) {
            Bukkit.getScheduler().cancelTask(activeTaskId);
            deactivateAnonymMode();
        }
    }
}