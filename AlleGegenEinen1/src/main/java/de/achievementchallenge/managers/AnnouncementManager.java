package de.achievementchallenge.managers;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Verwaltet Ankündigungen und Texte aus announcement.yml
 *
 * Lädt verschiedene Kategorien von Nachrichten:
 * - daemon_summon: Dämon-Beschwörung
 * - anonym: Anonym-Command
 * - audiotest: Audiotest-Command
 * - kaffepause: Kaffeepause-Ausreden
 * - lag: Lag-Texte
 * - befreimich: Befreiungs-Texte
 */
public class AnnouncementManager {

    private final AchievementChallengePlugin plugin;
    private FileConfiguration announcementConfig;
    private final Random random = new Random();

    public AnnouncementManager(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
        loadAnnouncements();
    }

    /**
     * Lädt die announcement.yml
     */
    private void loadAnnouncements() {
        File announcementFile = new File(plugin.getDataFolder(), "announcement.yml");

        // Erstelle Default-Datei wenn nicht vorhanden
        if (!announcementFile.exists()) {
            plugin.saveResource("announcement.yml", false);
        }

        announcementConfig = YamlConfiguration.loadConfiguration(announcementFile);
        plugin.getLogger().info("Announcements geladen");
    }

    /**
     * Gibt eine zufällige Ankündigung aus einer Kategorie zurück
     *
     * @param category Die Kategorie (z.B. "daemon_summon")
     * @return Zufällige Ankündigung aus der Kategorie
     */
    public String getRandomAnnouncement(String category) {
        List<String> announcements = announcementConfig.getStringList(category);

        if (announcements.isEmpty()) {
            plugin.getLogger().warning("Keine Announcements für Kategorie: " + category);
            return "§cFehler: Keine Ankündigung gefunden!";
        }

        return announcements.get(random.nextInt(announcements.size()));
    }

    /**
     * Gibt alle Ankündigungen einer Kategorie zurück
     */
    public List<String> getAllAnnouncements(String category) {
        return new ArrayList<>(announcementConfig.getStringList(category));
    }

    /**
     * Lädt die Announcements neu
     */
    public void reload() {
        loadAnnouncements();
    }
}