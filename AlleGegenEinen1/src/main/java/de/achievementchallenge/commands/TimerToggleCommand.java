package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerToggleCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public TimerToggleCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis Command can only be executed by Players!");
            return true;
        }

        Player player = (Player) sender;
        DataManager dm = plugin.getDataManager();

        dm.toggleActionBar(player.getUniqueId());
        dm.saveData();

        boolean enabled = dm.hasActionBarEnabled(player.getUniqueId());
        if (enabled) {
            player.sendMessage("§aActionBar enabled!");
        } else {
            player.sendMessage("§cActionBar disabled!");
        }

        return true;
    }
}