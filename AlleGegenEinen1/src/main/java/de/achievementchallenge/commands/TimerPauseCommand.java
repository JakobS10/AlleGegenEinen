package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerPauseCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public TimerPauseCommand(AchievementChallengePlugin plugin) {
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

        DataManager dm = plugin.getDataManager();

        if (!dm.isTimerRunning()) {
            player.sendMessage("§cTimer is not running!");
            return true;
        }

        dm.pauseTimer();
        dm.saveData();
        player.sendMessage("§eTimer stopped at: §6" + DataManager.formatTime(dm.getTimerTicks()));

        return true;
    }
}