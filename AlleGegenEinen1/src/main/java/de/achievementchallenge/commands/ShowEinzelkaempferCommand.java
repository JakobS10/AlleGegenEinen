package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.ChallengeManager;
import de.achievementchallenge.managers.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command: /showeinzelkaempfer
 *
 * Zeigt den aktuellen Einzelkämpfer und den Challenge-Status an.
 * Kann von allen Spielern verwendet werden.
 */
public class ShowEinzelkaempferCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public ShowEinzelkaempferCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ChallengeManager cm = plugin.getChallengeManager();

        // Prüfe ob Challenge aktiv ist
        if (!cm.isChallengeActive()) {
            sender.sendMessage("§c§lKeine aktive Challenge!");
            sender.sendMessage("§7Ein OP kann mit §e/seteinzelkaempfer <spieler> §7eine Challenge starten.");
            return true;
        }

        // Hole Daten
        String einzelkaempferName = cm.getEinzelkaempferName();
        int einzelkaempferCount = cm.getEinzelkaempferAchievementCount();
        int othersCount = cm.getOthersAchievementCount();

        // Hole Timer-Info
        DataManager dm = plugin.getDataManager();
        long currentTime = dm.getTimerTicks();
        long totalTime = cm.getChallengeDurationTicks();
        long remainingTime = totalTime - currentTime;

        // Formatiere Zeiten
        String currentTimeStr = DataManager.formatTime(currentTime);
        String totalTimeStr = DataManager.formatTime(totalTime);
        String remainingTimeStr = remainingTime > 0 ? DataManager.formatTime(remainingTime) : "§c0s";

        // Ausgabe
        sender.sendMessage("§6§l=== CHALLENGE STATUS ===");
        sender.sendMessage("");
        sender.sendMessage("§7Einzelkämpfer: §c§l" + einzelkaempferName);
        sender.sendMessage("§7Achievements: §e" + einzelkaempferCount + " §7vs. §e" + othersCount);
        sender.sendMessage("");
        sender.sendMessage("§7Zeit vergangen: §e" + currentTimeStr);
        sender.sendMessage("§7Verbleibende Zeit: §e" + remainingTimeStr);
        sender.sendMessage("§7Gesamtzeit: §e" + totalTimeStr);
        sender.sendMessage("");

        // Wer liegt vorne?
        if (einzelkaempferCount > othersCount) {
            sender.sendMessage("§a" + einzelkaempferName + " liegt vorne!");
        } else if (einzelkaempferCount < othersCount) {
            sender.sendMessage("§cDie anderen Spieler liegen vorne!");
        } else {
            sender.sendMessage("§eGleichstand!");
        }

        sender.sendMessage("§6§l=======================");

        return true;
    }
}