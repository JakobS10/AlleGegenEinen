package de.achievementchallenge.listeners;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.commands.ChallengeStatusCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Behandelt Klicks in den Challenge-GUIs:
 * - Einzelkämpfer setzen Bestätigung
 * - Einzelkämpfer setzen Zeitauswahl
 * - Challenge Reset Bestätigung
 */
public class ChallengeInventoryClickListener implements Listener {

    private final AchievementChallengePlugin plugin;

    // Speichert temporär, welcher Spieler für welchen Executor gesetzt werden soll
    // Map<Executor-UUID, Target-Player-Name>
    private final Map<UUID, String> pendingEinzelkaempfer = new HashMap<>();

    // NEU: Speichert die aktuell ausgewählten Zeiten für jeden Spieler
    // Map<Executor-UUID, TimeSelection>
    private final Map<UUID, TimeSelection> currentTimeSelection = new HashMap<>();

    public ChallengeInventoryClickListener(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Hilfsklasse zum Speichern der ausgewählten Zeit
     */
    private static class TimeSelection {
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // Einzelkämpfer setzen - Bestätigung
        if (title.contains("Einzelkämpfer setzen?")) {
            event.setCancelled(true);
            handleSetEinzelkaempferConfirmation(player, event);
        }
        // Einzelkämpfer setzen - Zeitauswahl
        else if (title.contains("Challenge-Zeit festlegen")) {
            event.setCancelled(true);
            handleTimeSelection(player, event);
        }
        // Challenge Reset - Bestätigung
        else if (title.contains("Challenge zurücksetzen?")) {
            event.setCancelled(true);
            handleResetChallengeConfirmation(player, event);
        }
        // Challenge Status - Ansichtswechsel
        else if (title.contains("Challenge Status")) {
            event.setCancelled(true);
            handleChallengeStatusClick(player, event);
        }
    }

    /**
     * Behandelt Klicks in der Einzelkämpfer-Bestätigungs-GUI
     */
    private void handleSetEinzelkaempferConfirmation(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.LIME_WOOL) {
            // Bestätigt - Hole Zielspieler aus dem Kopf-Item
            ItemStack headItem = event.getInventory().getItem(13);
            if (headItem != null && headItem.getType() == Material.PLAYER_HEAD) {
                org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) headItem.getItemMeta();
                String targetName = meta.getOwningPlayer().getName();

                // Speichere für Zeitauswahl
                pendingEinzelkaempfer.put(player.getUniqueId(), targetName);

                // Initialisiere leere TimeSelection
                currentTimeSelection.put(player.getUniqueId(), new TimeSelection());

                // Öffne Zeitauswahl-GUI
                player.closeInventory();
                openTimeSelectionGUI(player, targetName);
            }
        } else if (clicked.getType() == Material.RED_WOOL) {
            // Abgebrochen
            player.closeInventory();
            player.sendMessage("§cAbgebrochen.");
        }
    }

    /**
     * Öffnet die GUI zur Zeitauswahl
     */
    private void openTimeSelectionGUI(Player player, String targetName) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6Challenge-Zeit festlegen");

        // Info-Item oben (zeigt aktuelle Auswahl)
        TimeSelection selection = currentTimeSelection.getOrDefault(player.getUniqueId(), new TimeSelection());

        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§lZeit für die Challenge");
        infoMeta.setLore(Arrays.asList(
                "§7Wähle die Dauer der Challenge:",
                "§7Klicke auf Wollblöcke zum Auswählen",
                "",
                "§7Ziel-Spieler: §e" + targetName,
                "",
                "§6Aktuelle Auswahl:",
                "§e" + selection.days + " Tage, " + selection.hours + " Stunden, "
                        + selection.minutes + " Minuten, " + selection.seconds + " Sekunden"
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Zeit-Auswahl Items
        // Tage (Slot 10-16)
        for (int i = 0; i <= 7; i++) {
            Material mat = (selection.days == i) ? Material.LIME_WOOL : Material.BLUE_WOOL;
            inv.setItem(10 + i, createTimeItem(mat, i + " Tage", "days:" + i));
        }

        // Stunden (Slot 19-25)
        for (int i = 0; i <= 23; i += 3) { // 0, 3, 6, 9, 12, 15, 18, 21
            Material mat = (selection.hours == i) ? Material.LIME_WOOL : Material.GREEN_WOOL;
            inv.setItem(19 + (i / 3), createTimeItem(mat, i + " Stunden", "hours:" + i));
        }

        // Minuten (Slot 28-34)
        for (int i = 0; i <= 60; i += 10) { // 0, 10, 20, 30, 40, 50, 60
            Material mat = (selection.minutes == i) ? Material.LIME_WOOL : Material.YELLOW_WOOL;
            inv.setItem(28 + (i / 10), createTimeItem(mat, i + " Minuten", "minutes:" + i));
        }

        // Sekunden (Slot 37-43)
        for (int i = 0; i <= 60; i += 10) {
            Material mat = (selection.seconds == i) ? Material.LIME_WOOL : Material.RED_WOOL;
            inv.setItem(37 + (i / 10), createTimeItem(mat, i + " Sekunden", "seconds:" + i));
        }

        // Bestätigen-Button (Slot 49)
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lChallenge starten");
        confirmMeta.setLore(Arrays.asList(
                "§7Startet die Challenge mit:",
                "§e" + selection.days + " Tage, " + selection.hours + " Stunden,",
                "§e" + selection.minutes + " Minuten, " + selection.seconds + " Sekunden"
        ));
        confirm.setItemMeta(confirmMeta);
        inv.setItem(49, confirm);

        // Abbrechen-Button (Slot 53)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lAbbrechen");
        cancelMeta.setLore(Arrays.asList("§7Klicke zum Abbrechen"));
        cancel.setItemMeta(cancelMeta);
        inv.setItem(53, cancel);

        player.openInventory(inv);
    }

    /**
     * Erstellt ein Zeit-Auswahl-Item
     */
    private ItemStack createTimeItem(Material material, String displayName, String data) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + displayName);
        meta.setLore(Arrays.asList("§7Klicke zum Auswählen", "§8" + data)); // Data in Lore für später
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Behandelt Klicks in der Zeitauswahl-GUI
     */
    private void handleTimeSelection(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Hole Ziel-Spieler
        String targetName = pendingEinzelkaempfer.get(player.getUniqueId());
        if (targetName == null) {
            player.closeInventory();
            player.sendMessage("§cFehler: Zielspieler nicht gefunden!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.closeInventory();
            player.sendMessage("§cSpieler ist nicht mehr online: " + targetName);
            pendingEinzelkaempfer.remove(player.getUniqueId());
            currentTimeSelection.remove(player.getUniqueId());
            return;
        }

        // Bestätigen-Button (Slot 49)
        if (event.getSlot() == 49 && clicked.getType() == Material.LIME_WOOL) {
            TimeSelection selection = currentTimeSelection.get(player.getUniqueId());
            if (selection == null) {
                selection = new TimeSelection();
            }

            startChallenge(player, target, selection.days, selection.hours, selection.minutes, selection.seconds);
            return;
        }

        // Abbrechen-Button (Slot 53)
        if (event.getSlot() == 53 && clicked.getType() == Material.RED_WOOL) {
            player.closeInventory();
            player.sendMessage("§cAbgebrochen.");
            pendingEinzelkaempfer.remove(player.getUniqueId());
            currentTimeSelection.remove(player.getUniqueId());
            return;
        }

        // Parse Zeitwahl aus Lore
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getLore() == null || meta.getLore().size() < 2) {
            return;
        }

        String data = meta.getLore().get(1).replace("§8", "");
        String[] parts = data.split(":");
        if (parts.length != 2) {
            return;
        }

        String type = parts[0];
        int value = Integer.parseInt(parts[1]);

        // Aktualisiere die Auswahl
        TimeSelection selection = currentTimeSelection.computeIfAbsent(player.getUniqueId(), k -> new TimeSelection());

        switch (type) {
            case "days":
                selection.days = value;
                break;
            case "hours":
                selection.hours = value;
                break;
            case "minutes":
                selection.minutes = value;
                break;
            case "seconds":
                selection.seconds = value;
                break;
        }

        // GUI neu öffnen um Änderungen anzuzeigen
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openTimeSelectionGUI(player, targetName);
        }, 1L);
    }

    /**
     * Startet die Challenge mit den gewählten Parametern
     */
    private void startChallenge(Player executor, Player target, int days, int hours, int minutes, int seconds) {
        executor.closeInventory();

        // Entferne aus pending
        pendingEinzelkaempfer.remove(executor.getUniqueId());
        currentTimeSelection.remove(executor.getUniqueId());

        // Starte Challenge
        plugin.getChallengeManager().setEinzelkaempfer(target, days, hours, minutes, seconds);

        // Starte Challenge-Check im TimerManager
        plugin.getTimerManager().startChallengeCheck();

        executor.sendMessage("§aChallenge wurde gestartet!");
    }

    /**
     * Behandelt Klicks in der Challenge-Reset-Bestätigungs-GUI
     */
    private void handleResetChallengeConfirmation(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.LIME_WOOL) {
            // Bestätigt - Reset Challenge
            player.closeInventory();
            plugin.getChallengeManager().resetChallenge();
            plugin.getTimerManager().stopChallengeCheck();
            player.sendMessage("§aChallenge wurde zurückgesetzt!");
        } else if (clicked.getType() == Material.RED_WOOL) {
            // Abgebrochen
            player.closeInventory();
            player.sendMessage("§cAbgebrochen.");
        }
    }

    /**
     * Behandelt Klicks in der Challenge-Status-GUI (für Ansichtswechsel)
     */
    private void handleChallengeStatusClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // Prüfe ob der Filter-Button (Hopper) geklickt wurde
        if (clicked.getType() == Material.HOPPER && event.getSlot() == 53) {
            // Wechsle Ansicht
            ChallengeStatusCommand cmd = (ChallengeStatusCommand) plugin.getCommand("challengestatus").getExecutor();
            cmd.toggleViewMode(player);

            player.closeInventory();

            // Öffne GUI mit neuer Ansicht
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cmd.openStatusGUI(player);
            }, 1L);
        }
    }
}