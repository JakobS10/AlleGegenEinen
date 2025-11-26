package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.*;

public class SchattenCommand implements CommandExecutor, Listener {

    private final AchievementChallengePlugin plugin;
    private final Map<UUID, ShadowData> activeShadows = new HashMap<>();

    public SchattenCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern ausgeführt werden!");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getDaemonManager().checkDaemonPermission(player)) return true;

        if (args.length != 1) {
            player.sendMessage("§cFalsche Verwendung!");
            player.sendMessage("§7Verwendung: /schatten <Spieler>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        if (activeShadows.containsKey(target.getUniqueId())) {
            deactivateShadow(target, player);
        } else {
            activateShadow(target, player);
        }

        return true;
    }

    private void activateShadow(Player target, Player executor) {
        String targetName = target.getName();

        String announcement = plugin.getAnnouncementManager().getRandomAnnouncement("schatten");
        announcement = announcement.replace("{target}", targetName);

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + announcement);
        Bukkit.broadcastMessage("");

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        }

        target.sendMessage("§8§lEin Schatten folgt dir...");
        target.sendMessage("§7Er kopiert jede deiner Bewegungen...");

        if (executor != null) executor.sendMessage("§a✓ Schatten aktiviert für " + targetName + "!");

        NPC npc = CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.PLAYER, targetName + "'s Schatten");
        npc.spawn(target.getLocation());

        SkinTrait skin = npc.getOrAddTrait(SkinTrait.class);
        skin.setShouldUpdateSkins(true);
        skin.setSkinName(target.getName());

        ShadowData data = new ShadowData(target, npc);
        activeShadows.put(target.getUniqueId(), data);

        startMovementTracking(data);

        plugin.getLogger().info("Schatten aktiviert für " + targetName);
    }

    private void startMovementTracking(ShadowData data) {
        data.recordTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!data.target.isOnline()) {
                deactivateShadow(data.target, null);
                return;
            }

            Location loc = data.target.getLocation().clone();
            data.locationHistory.add(new LocationSnapshot(loc, System.currentTimeMillis()));

            while (!data.locationHistory.isEmpty()) {
                LocationSnapshot oldest = data.locationHistory.peek();
                if (System.currentTimeMillis() - oldest.timestamp > 2500) {
                    data.locationHistory.poll();
                } else break;
            }

        }, 0L, 2L).getTaskId();

        data.updateTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!data.target.isOnline()) return;

            long targetTime = System.currentTimeMillis() - 2000;
            LocationSnapshot snapshot = null;

            for (LocationSnapshot s : data.locationHistory) {
                if (s.timestamp <= targetTime) snapshot = s;
                else break;
            }

            if (snapshot != null && data.npc.isSpawned()) {
                data.npc.teleport(snapshot.location, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
            }

        }, 40L, 2L).getTaskId();
    }

    private void deactivateShadow(Player target, Player executor) {
        ShadowData data = activeShadows.remove(target.getUniqueId());
        if (data == null) {
            if (executor != null) executor.sendMessage("§c" + target.getName() + " hat keinen Schatten!");
            return;
        }

        if (data.recordTaskId != -1) Bukkit.getScheduler().cancelTask(data.recordTaskId);
        if (data.updateTaskId != -1) Bukkit.getScheduler().cancelTask(data.updateTaskId);

        if (data.npc != null && data.npc.isSpawned()) data.npc.destroy();

        if (target.isOnline()) {
            target.sendMessage("§a§lDein Schatten ist verschwunden!");
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }

        if (executor != null) executor.sendMessage("§a✓ Schatten entfernt von " + target.getName() + "!");

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7" + target.getName() + "'s Schatten ist verschwunden!");
        Bukkit.broadcastMessage("");

        plugin.getLogger().info("Schatten deaktiviert für " + target.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeShadows.containsKey(player.getUniqueId())) {
            deactivateShadow(player, null);
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            stopAll();
        }
    }

    public void stopAll() {
        for (UUID uuid : new HashSet<>(activeShadows.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) deactivateShadow(p, null);
        }
        activeShadows.clear();
    }

    private static class ShadowData {
        final Player target;
        final NPC npc;
        final Queue<LocationSnapshot> locationHistory = new LinkedList<>();
        int recordTaskId = -1;
        int updateTaskId = -1;

        ShadowData(Player target, NPC npc) {
            this.target = target;
            this.npc = npc;
        }
    }

    private static class LocationSnapshot {
        final Location location;
        final long timestamp;

        LocationSnapshot(Location location, long timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
    }
}