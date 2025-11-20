package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Command: /resetchallenge
 *
 * Setzt die komplette Challenge zurück (nur für OPs).
 * Zeigt eine Bestätigungs-GUI an.
 */
public class ResetChallengeCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public ResetChallengeCommand(AchievementChallengePlugin plugin) {
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

        // Prüfe OP-Rechte
        if (!player.isOp()) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Command!");
            return true;
        }

        // Öffne Bestätigungs-GUI
        openConfirmationGUI(player);

        return true;
    }

    /**
     * Öffnet die Bestätigungs-GUI für Challenge-Reset
     *
     * @param player Der Spieler, der den Command ausführt
     */
    private void openConfirmationGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lChallenge zurücksetzen?");

        // Bestätigen-Item
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lBestätigen");
        confirmMeta.setLore(Arrays.asList(
                "§7Challenge komplett zurücksetzen",
                "",
                "§c§lWARNUNG:",
                "§7- Einzelkämpfer wird entfernt",
                "§7- Alle Achievements werden entfernt",
                "§7- Alle Inventare werden geleert",
                "§7- Timer wird zurückgesetzt",
                "",
                "§c§lDies kann nicht rückgängig gemacht werden!"
        ));
        confirm.setItemMeta(confirmMeta);

        // Abbrechen-Item
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lAbbrechen");
        cancelMeta.setLore(Arrays.asList("§7Klicke zum Abbrechen"));
        cancel.setItemMeta(cancelMeta);

        // Items platzieren
        inv.setItem(11, confirm);
        inv.setItem(15, cancel);

        player.openInventory(inv);
    }
}