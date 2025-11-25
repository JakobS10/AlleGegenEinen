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
 * - Challenge Status Ansichtswechsel
 * - Challenge Status Pagination (Vor/Zurück)
 */
public class ChallengeInventoryClickListener implements Listener {

    private final AchievementChallengePlugin plugin;

    // Speichert temporär, welcher Spieler für welchen Executor gesetzt werden soll
    // Map<Executor-UUID, Target-Player-Name>
    private final Map<UUID, String> pendingEinzelkaempfer = new HashMap<>();

    public ChallengeInventoryClickListener(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
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
        // Challenge Status - Alle Interaktionen
        else if (title.contains("Challenge Status") || title.contains("Achievements - Seite")) {
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

        // Info-Item oben
        ItemStack info = new ItemStack(Material.CLOCK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§lZeit für die Challenge");
        infoMeta.setLore(Arrays.asList(
                "§7Wähle die Dauer der Challenge:",
                "§7Tage | Stunden | Minuten | Sekunden",
                "",
                "§7Ziel-Spieler: §e" + targetName
        ));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Zeit-Auswahl Items
        // Tage (Slot 10-16)
        for (int i = 0; i <= 7; i++) {
            inv.setItem(10 + i, createTimeItem(Material.BLUE_WOOL, i + " Tage", "days:" + i));
        }

        // Stunden (Slot 19-25)
        for (int i = 0; i <= 23; i += 3) { // 0, 3, 6, 9, 12, 15, 18, 21
            inv.setItem(19 + (i / 3), createTimeItem(Material.GREEN_WOOL, i + " Stunden", "hours:" + i));
        }

        // Minuten (Slot 28-34)
        for (int i = 0; i <= 60; i += 10) { // 0, 10, 20, 30, 40, 50, 60
            inv.setItem(28 + (i / 10), createTimeItem(Material.YELLOW_WOOL, i + " Minuten", "minutes:" + i));
        }

        // Sekunden (Slot 37-43)
        for (int i = 0; i <= 60; i += 10) {
            inv.setItem(37 + (i / 10), createTimeItem(Material.RED_WOOL, i + " Sekunden", "seconds:" + i));
        }

        // Standard-Auswahl (Slot 49)
        ItemStack standard = new ItemStack(Material.LIME_WOOL);
        ItemMeta standardMeta = standard.getItemMeta();
        standardMeta.setDisplayName("§a§lStandard: 1 Stunde");
        standardMeta.setLore(Arrays.asList(
                "§7Setzt die Challenge auf 1 Stunde",
                "§7(0 Tage, 1 Stunde, 0 Minuten, 0 Sekunden)"
        ));
        standard.setItemMeta(standardMeta);
        inv.setItem(49, standard);

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
            return;
        }

        // Standard-Auswahl (1 Stunde)
        if (clicked.getType() == Material.LIME_WOOL) {
            startChallenge(player, target, 0, 1, 0, 0);
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

        // Verwende nur den ausgewählten Wert, keine Standard-Werte!
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        // Setze nur den gewählten Wert
        switch (type) {
            case "days":
                days = value;
                break;
            case "hours":
                hours = value;
                break;
            case "minutes":
                minutes = value;
                break;
            case "seconds":
                seconds = value;
                break;
        }

        startChallenge(player, target, days, hours, minutes, seconds);
    }

    /**
     * Startet die Challenge mit den gewählten Parametern
     */
    private void startChallenge(Player executor, Player target, int days, int hours, int minutes, int seconds) {
        executor.closeInventory();

        // Entferne aus pending
        pendingEinzelkaempfer.remove(executor.getUniqueId());

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
     * Behandelt Klicks in der Challenge-Status-GUI
     * Inkl. Ansichtswechsel UND Pagination
     */
    private void handleChallengeStatusClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();

        // Vorherige Seite (Slot 45) - nur in Achievement-Ansicht
        if (slot == 45 && clicked.getType() == Material.ARROW) {
            ChallengeStatusCommand cmd = (ChallengeStatusCommand) plugin.getCommand("challengestatus").getExecutor();
            cmd.previousPage(player);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cmd.openStatusGUI(player);
            }, 1L);
            return;
        }

        // Ansicht wechseln (Slot 48 in Achievement-Ansicht, Slot 53 in Spieler-Ansicht)
        if ((slot == 48 || slot == 53) && clicked.getType() == Material.HOPPER) {
            ChallengeStatusCommand cmd = (ChallengeStatusCommand) plugin.getCommand("challengestatus").getExecutor();
            cmd.toggleViewMode(player);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cmd.openStatusGUI(player);
            }, 1L);
            return;
        }

        // Nächste Seite (Slot 53) - nur in Achievement-Ansicht
        if (slot == 53 && clicked.getType() == Material.ARROW) {
            ChallengeStatusCommand cmd = (ChallengeStatusCommand) plugin.getCommand("challengestatus").getExecutor();
            cmd.nextPage(player);

            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cmd.openStatusGUI(player);
            }, 1L);
            return;
        }
    }
}