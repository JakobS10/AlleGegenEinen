package de.achievementchallenge.managers;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Verwaltet Dämon-Status von Spielern
 *
 * Dämonen haben Zugriff auf spezielle Troll-Commands:
 * - /anonym
 * - /audiotest
 * - /kaffepause
 * - /lag
 */
public class DaemonManager {

    private final AchievementChallengePlugin plugin;

    // Set aller Dämonen (UUID -> Name)
    private final Map<UUID, String> daemons = new HashMap<>();

    public DaemonManager(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Fügt einen Dämon hinzu
     */
    public void addDaemon(UUID uuid, String name) {
        daemons.put(uuid, name);
    }

    /**
     * Entfernt einen Dämon
     */
    public void removeDaemon(UUID uuid) {
        daemons.remove(uuid);
    }

    /**
     * Prüft ob ein Spieler ein Dämon ist
     */
    public boolean isDaemon(UUID uuid) {
        return daemons.containsKey(uuid);
    }

    /**
     * Gibt alle Dämonen zurück
     */
    public Map<UUID, String> getAllDaemons() {
        return new HashMap<>(daemons);
    }

    /**
     * Prüft ob ein Spieler ein Dämon ist und gibt ggf. Fehlermeldung
     *
     * @return true wenn Dämon, false wenn nicht
     */
    public boolean checkDaemonPermission(Player player) {
        if (!isDaemon(player.getUniqueId())) {
            player.sendMessage("§c§lZugriff verweigert!");
            player.sendMessage("§7Nur Dämonen können diesen Command nutzen!");
            player.sendMessage("§8Nutze §c/666 §8um ein Dämon zu werden...");
            return false;
        }
        return true;
    }

    /**
     * Lädt Dämon-Daten (für DataManager)
     */
    public void setData(Map<UUID, String> daemonData) {
        daemons.clear();
        daemons.putAll(daemonData);
    }

    /**
     * Löscht alle Dämonen
     */
    public void clearAll() {
        daemons.clear();
    }
}