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

public class TimerResetCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public TimerResetCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis Command can only be executed by Players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cYou are not allowed to execute this Command!");
            return true;
        }

        openConfirmationGUI(player, "timerreset");

        return true;
    }

    private void openConfirmationGUI(Player player, String type) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lReset Timer?");

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lConfirm");
        confirmMeta.setLore(Arrays.asList("§7Klick to reset timer"));
        confirm.setItemMeta(confirmMeta);

        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lCancel");
        cancelMeta.setLore(Arrays.asList("§7Klick to cancel"));
        cancel.setItemMeta(cancelMeta);

        inv.setItem(11, confirm);
        inv.setItem(15, cancel);

        player.openInventory(inv);
    }
}