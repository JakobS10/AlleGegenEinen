package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.ChallengeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Command: /challengestatus
 *
 * Öffnet eine GUI mit Spielerköpfen und zeigt den aktuellen Challenge-Status.
 * Kann von allen Spielern verwendet werden.
 */
public class ChallengeStatusCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public ChallengeStatusCommand(AchievementChallengePlugin plugin) {
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
        ChallengeManager cm = plugin.getChallengeManager();

        // Prüfe ob Challenge aktiv ist
        if (!cm.isChallengeActive()) {
            player.sendMessage("§c§lKeine aktive Challenge!");
            player.sendMessage("§7Ein OP kann mit §e/seteinzelkaempfer <spieler> §7eine Challenge starten.");
            return true;
        }

        // Öffne GUI
        openStatusGUI(player);

        return true;
    }

    /**
     * Öffnet die Status-GUI mit Spielerköpfen
     *
     * @param player Der Spieler, der die GUI sieht
     */
    public void openStatusGUI(Player player) {
        ChallengeManager cm = plugin.getChallengeManager();

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lChallenge Status");

        // Einzelkämpfer-Kopf (Slot 13)
        OfflinePlayer einzelkaempfer = cm.getEinzelkaempferOffline();
        if (einzelkaempfer != null) {
            ItemStack head = createPlayerHead(
                    einzelkaempfer,
                    "§c§lEinzelkämpfer: §e" + cm.getEinzelkaempferName(),
                    cm.getEinzelkaempferAchievementCount()
            );
            inv.setItem(13, head);
        }

        // "Andere Spieler" Überschrift (Slot 31)
        ItemStack separator = new ItemStack(Material.IRON_BARS);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.setDisplayName("§7§l--- VS ---");
        separator.setItemMeta(sepMeta);
        inv.setItem(31, separator);

        // Alle anderen Spieler (ab Slot 36)
        List<Player> others = cm.getOtherPlayers();
        int slot = 36;

        for (Player p : others) {
            if (slot >= 45) break; // Maximal 9 Spieler in der untersten Reihe

            int achievementCount = plugin.getAchievementProgressManager().getCompletedCount(p.getUniqueId());
            ItemStack head = createPlayerHead(p, "§a" + p.getName(), achievementCount);
            inv.setItem(slot, head);
            slot++;
        }

        // Info-Item (Slot 49)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lChallenge Info");

        List<String> lore = new ArrayList<>();
        lore.add("§7Einzelkämpfer: §e" + cm.getEinzelkaempferAchievementCount() + " Achievements");
        lore.add("§7Alle anderen: §e" + cm.getOthersAchievementCount() + " verschiedene Achievements");
        lore.add("");

        int diff = cm.getEinzelkaempferAchievementCount() - cm.getOthersAchievementCount();
        if (diff > 0) {
            lore.add("§a" + cm.getEinzelkaempferName() + " liegt " + diff + " vorne!");
        } else if (diff < 0) {
            lore.add("§cDie anderen liegen " + Math.abs(diff) + " vorne!");
        } else {
            lore.add("§eGleichstand!");
        }

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        player.openInventory(inv);
    }

    /**
     * Erstellt einen Spielerkopf mit Achievement-Info
     *
     * @param offlinePlayer Der Spieler
     * @param displayName Anzeigename
     * @param achievementCount Anzahl der Achievements
     * @return ItemStack mit Spielerkopf
     */
    private ItemStack createPlayerHead(OfflinePlayer offlinePlayer, String displayName, int achievementCount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Achievements: §e" + achievementCount);
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
    }
}