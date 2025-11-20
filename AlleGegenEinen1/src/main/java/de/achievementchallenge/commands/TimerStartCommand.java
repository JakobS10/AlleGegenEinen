package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimerStartCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public TimerStartCommand(AchievementChallengePlugin plugin) {
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

        if (dm.isTimerRunning()) {
            player.sendMessage("§cTimer is already running!");
            return true;
        }

        // Prüfe ob Argumente angegeben wurden
        if (args.length == 4) {
            try {
                int days = Integer.parseInt(args[0]);
                int hours = Integer.parseInt(args[1]);
                int minutes = Integer.parseInt(args[2]);
                int seconds = Integer.parseInt(args[3]);

                // Validierung
                if (days < 0 || hours < 0 || hours >= 24 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
                    player.sendMessage("§cWrong usage!");
                    player.sendMessage("§7FUsage: /timerstart <Days> <Hours(0-23)> <Minutes(0-59)> <Seconds(0-59)>");
                    return true;
                }

                // Berechne Ticks (20 Ticks = 1 Sekunde)
                long totalSeconds = (days * 86400L) + (hours * 3600L) + (minutes * 60L) + seconds;
                long ticks = totalSeconds * 20L;

                dm.setTimerTicks(ticks);
                dm.startTimer();
                dm.saveData();

                String timeStr = DataManager.formatTime(ticks);
                player.sendMessage("§aTimer started at §e" + timeStr + " §a!");

            } catch (NumberFormatException e) {
                player.sendMessage("§cWrong usage!");
                player.sendMessage("§7Usage: /timerstart §eor §7/timerstart <Days> <Hours> <Minutes> <Seconds>");
                return true;
            }
        } else if (args.length == 0) {
            // Standard: Timer bei 0 starten
            dm.startTimer();
            dm.saveData();
            player.sendMessage("§aTimer started!");
        } else {
            player.sendMessage("§cWrong Usage!");
            player.sendMessage("§7Usage: /timerstart §eor §7/timerstart <Days> <Hours> <Minutes> <Seconds>");
            return true;
        }

        return true;
    }
}