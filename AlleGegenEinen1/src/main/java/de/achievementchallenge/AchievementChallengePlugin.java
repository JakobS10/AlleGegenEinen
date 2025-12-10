package de.achievementchallenge;

import de.achievementchallenge.commands.*;
import de.achievementchallenge.listeners.*;
import de.achievementchallenge.managers.*;
import de.achievementchallenge.utils.AchievementRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Hauptklasse für das Achievement Challenge Plugin
 *
 * Dieses Plugin implementiert eine Challenge, bei der ein Einzelkämpfer
 * versucht, mehr verschiedene Achievements zu schaffen als alle anderen
 * Spieler zusammen.
 */
public class AchievementChallengePlugin extends JavaPlugin {

    private static AchievementChallengePlugin instance;

    // Manager
    private DataManager dataManager;
    private TimerManager timerManager;
    private ChallengeManager challengeManager;
    private AchievementProgressManager achievementProgressManager;


    @Override
    public void onEnable() {
        instance = this;

        // Erstelle Plugin-Ordner falls nicht vorhanden
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialisiere Achievement-Registry (lädt trackedAchievements.yml)
        AchievementRegistry.initialize(getDataFolder());

        // Erstelle/lade Konfiguration
        saveDefaultConfig();

        // Initialisiere Manager in der richtigen Reihenfolge
        dataManager = new DataManager(this);
        achievementProgressManager = new AchievementProgressManager(this);
        challengeManager = new ChallengeManager(this);
        timerManager = new TimerManager(this);

        // Lade gespeicherte Daten
        dataManager.loadData();

        // Registriere Commands
        registerCommands();

        // Registriere Listener
        registerListeners();

        // ActionBar Update Task (alle 20 Ticks = 1 Sekunde)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            timerManager.updateActionBars();
        }, 0L, 20L);

        // Auto-Save Task (alle 5 Minuten)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            Bukkit.getScheduler().runTask(this, () -> {
                dataManager.saveData();
            });
        }, 6000L, 6000L);

        getLogger().info("Achievement Challenge Plugin wurde aktiviert!");
        getLogger().info("Tracking: " + AchievementRegistry.getTotalAchievementCount() + " Achievements");
    }

    @Override
    public void onDisable() {
        // Speichere alle Daten
        dataManager.saveData();

        // Stoppe Challenge-Check falls aktiv
        if (timerManager != null) {
            timerManager.stopChallengeCheck();
        }

        getLogger().info("Achievement Challenge Plugin wurde deaktiviert!");
    }

    /**
     * Registriert alle Commands
     */
    private void registerCommands() {
        // Timer-Commands (aus MobHunt übernommen)
        getCommand("timerstart").setExecutor(new TimerStartCommand(this));
        getCommand("timerpause").setExecutor(new TimerPauseCommand(this));
        getCommand("timerresume").setExecutor(new TimerResumeCommand(this));
        getCommand("timer").setExecutor(new TimerCommand(this));
        getCommand("timerreset").setExecutor(new TimerResetCommand(this));
        getCommand("timertoggle").setExecutor(new TimerToggleCommand(this));

        // Challenge-Commands (neu)
        getCommand("seteinzelkaempfer").setExecutor(new SetEinzelkaempferCommand(this));
        getCommand("showeinzelkaempfer").setExecutor(new ShowEinzelkaempferCommand(this));
        getCommand("challengestatus").setExecutor(new ChallengeStatusCommand(this));
        getCommand("resetchallenge").setExecutor(new ResetChallengeCommand(this));
    }
    /**
     * Registriert alle Listener
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new AchievementListener(this), this);
        getServer().getPluginManager().registerEvents(new ChallengeInventoryClickListener(this), this);
    }

    // ==================== Getter ====================

    public static AchievementChallengePlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public ChallengeManager getChallengeManager() {
        return challengeManager;
    }

    public AchievementProgressManager getAchievementProgressManager() {
        return achievementProgressManager;
    }
}