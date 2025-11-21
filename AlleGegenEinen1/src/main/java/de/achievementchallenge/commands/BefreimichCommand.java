package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Command: /befreimich
 *
 * Gibt allen Dämonen ein unsichtbares Totem in die Offhand.
 * Wenn es gepoppt wird (Spieler stirbt), wird der Dämon-Status entfernt.
 */
public class BefreimichCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public BefreimichCommand(AchievementChallengePlugin plugin) {
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

        // Gib allen Dämonen das Befreiungs-Totem
        Map<UUID, String> daemons = plugin.getDaemonManager().getAllDaemons();
        int given = 0;

        for (UUID daemonUUID : daemons.keySet()) {
            Player daemon = Bukkit.getPlayer(daemonUUID);
            if (daemon != null && daemon.isOnline()) {
                giveBefreiungTotem(daemon);
                given++;
            }
        }

        // Broadcast
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e§l⚪ BEFREIUNG VERFÜGBAR ⚪");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§7Alle Dämonen können jetzt befreit werden, indem ihr sie einal §aumbringt §7!");
        Bukkit.broadcastMessage("");

        // Sound
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 1.2f);
        }

        plugin.getLogger().info("Befreiungs-Totems vergeben an " + given + " Dämonen");

        return true;
    }

    /**
     * Gibt einem Dämon das Befreiungs-Totem
     */
    private void giveBefreiungTotem(Player daemon) {
        // Erstelle unsichtbares Totem
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();

        meta.setDisplayName("§6§lBefreiungs-Totem");
        meta.setLore(Arrays.asList(
                "§7Stirb mit diesem Totem",
                "§7um deinen Dämon-Status",
                "§7zu verlieren.",
                "",
                "§8Unsichtbar für andere..."
        ));

        // Mache es "special" mit Enchant-Glanz
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        totem.setItemMeta(meta);

        // Setze in Offhand
        daemon.getInventory().setItemInOffHand(totem);

        // Nachricht für Dämon
        daemon.sendMessage("§e§lDu hast ein Befreiungs-Totem erhalten!");
        daemon.sendMessage("§7Stirb damit um deine dunklen Kräfte aufzugeben.");
        daemon.playSound(daemon.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.5f);
    }

    /**
     * Prüft ob ein Item ein Befreiungs-Totem ist
     */
    public static boolean isBefreiungTotem(ItemStack item) {
        if (item == null || item.getType() != Material.TOTEM_OF_UNDYING) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals("§6§lBefreiungs-Totem");
    }
}