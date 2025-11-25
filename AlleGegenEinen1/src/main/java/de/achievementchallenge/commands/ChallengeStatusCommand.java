package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import de.achievementchallenge.managers.ChallengeManager;
import de.achievementchallenge.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Command: /challengestatus
 *
 * Öffnet eine GUI mit zwei Ansichts-Modi:
 * - Spieler-Ansicht: Zeigt Einzelkämpfer vs. alle anderen Spieler
 * - Achievement-Ansicht: Zeigt alle Achievements mit detaillierten Infos + PAGINATION
 *
 * Kann von allen Spielern verwendet werden.
 */
public class ChallengeStatusCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    // Speichert den aktuellen Ansichts-Modus pro Spieler
    private final Map<UUID, ViewMode> playerViewMode = new HashMap<>();

    // Speichert die aktuelle Seite pro Spieler (für Achievement-Ansicht)
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();

    // Achievements pro Seite
    private static final int ACHIEVEMENTS_PER_PAGE = 45; // Slots 0-44

    public ChallengeStatusCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ansichts-Modi für die GUI
     */
    public enum ViewMode {
        PLAYERS("Spieler-Ansicht"),
        ACHIEVEMENTS("Achievement-Ansicht");

        private final String displayName;

        ViewMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Prüfe ob Sender ein Spieler ist
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern ausgeführt werden!");
            return true;
        }

        Player player = (Player) sender;
        ChallengeManager cm = plugin.getChallengeManager();

        // Prüfe ob Challenge aktiv ist
        if (!cm.isChallengeActive()) {
            player.sendMessage("§c§lKeine aktive Challenge!");
            player.sendMessage("§7Ein OP kann mit §e/seteinzelkaempfer <spieler> §7eine Challenge starten.");
            return true;
        }

        // Initialisiere ViewMode falls noch nicht vorhanden
        playerViewMode.putIfAbsent(player.getUniqueId(), ViewMode.PLAYERS);
        playerCurrentPage.putIfAbsent(player.getUniqueId(), 0);

        // Öffne GUI
        openStatusGUI(player);

        return true;
    }

    /**
     * Öffnet die Status-GUI basierend auf dem aktuellen ViewMode
     *
     * @param player Der Spieler, der die GUI sieht
     */
    public void openStatusGUI(Player player) {
        ViewMode mode = playerViewMode.get(player.getUniqueId());

        if (mode == ViewMode.PLAYERS) {
            openPlayerView(player);
        } else {
            openAchievementView(player);
        }
    }

    /**
     * Öffnet die Spieler-Ansicht
     */
    private void openPlayerView(Player player) {
        ChallengeManager cm = plugin.getChallengeManager();

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lChallenge Status - Spieler");

        // Einzelkämpfer-Kopf (Slot 13)
        OfflinePlayer einzelkaempfer = cm.getEinzelkaempferOffline();
        if (einzelkaempfer != null) {
            ItemStack head = createPlayerHead(
                    einzelkaempfer,
                    "§c§lEinzelkämpfer: §e" + cm.getEinzelkaempferName(),
                    cm.getEinzelkaempferAchievementCount()
            );
            inv.setItem(13, head);
        }

        // "Andere Spieler" Überschrift (Slot 31)
        ItemStack separator = new ItemStack(Material.IRON_BARS);
        ItemMeta sepMeta = separator.getItemMeta();
        sepMeta.setDisplayName("§7§l--- VS ---");
        separator.setItemMeta(sepMeta);
        inv.setItem(31, separator);

        // Alle anderen Spieler (ab Slot 36)
        List<Player> others = cm.getOtherPlayers();
        int slot = 36;

        for (Player p : others) {
            if (slot >= 45) break; // Maximal 9 Spieler in der untersten Reihe

            int achievementCount = plugin.getAchievementProgressManager().getCompletedCount(p.getUniqueId());
            ItemStack head = createPlayerHead(p, "§a" + p.getName(), achievementCount);
            inv.setItem(slot, head);
            slot++;
        }

        // Info-Item (Slot 49)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lChallenge Info");

        List<String> lore = new ArrayList<>();
        lore.add("§7Einzelkämpfer: §e" + cm.getEinzelkaempferAchievementCount() + " Achievements");
        lore.add("§7Alle anderen: §e" + cm.getOthersAchievementCount() + " verschiedene Achievements");
        lore.add("");

        int diff = cm.getEinzelkaempferAchievementCount() - cm.getOthersAchievementCount();
        if (diff > 0) {
            lore.add("§a" + cm.getEinzelkaempferName() + " liegt " + diff + " vorne!");
        } else if (diff < 0) {
            lore.add("§cDie anderen liegen " + Math.abs(diff) + " vorne!");
        } else {
            lore.add("§eGleichstand!");
        }

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        // Filter-Wechsel-Button (Slot 53)
        ItemStack filterButton = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filterButton.getItemMeta();
        filterMeta.setDisplayName("§6§lAnsicht wechseln");
        filterMeta.setLore(Arrays.asList(
                "§7Aktuell: §e" + ViewMode.PLAYERS.getDisplayName(),
                "",
                "§7Klicke um zur",
                "§e" + ViewMode.ACHIEVEMENTS.getDisplayName() + " §7zu wechseln"
        ));
        filterButton.setItemMeta(filterMeta);
        inv.setItem(53, filterButton);

        player.openInventory(inv);
    }

    /**
     * Öffnet die Achievement-Ansicht mit Pagination
     */
    private void openAchievementView(Player player) {
        ChallengeManager cm = plugin.getChallengeManager();
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);

        // Hole alle Achievements
        List<String> allAchievements = de.achievementchallenge.utils.AchievementRegistry.getAchievementKeys();
        int totalAchievements = allAchievements.size();
        int totalPages = (int) Math.ceil((double) totalAchievements / ACHIEVEMENTS_PER_PAGE);

        // Stelle sicher dass currentPage gültig ist
        if (currentPage < 0) currentPage = 0;
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        playerCurrentPage.put(player.getUniqueId(), currentPage);

        // Berechne Start- und End-Index für diese Seite
        int startIndex = currentPage * ACHIEVEMENTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ACHIEVEMENTS_PER_PAGE, totalAchievements);

        Inventory inv = Bukkit.createInventory(null, 54, "§6§lAchievements - Seite " + (currentPage + 1) + "/" + totalPages);

        // Zeige Achievements für diese Seite (Slots 0-44)
        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String key = allAchievements.get(i);
            ItemStack achievementItem = createAchievementStatusItem(key, cm);
            inv.setItem(slot, achievementItem);
            slot++;
        }

        // Navigation & Info (Slots 45-53)

        // Vorherige Seite (Slot 45)
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            prevMeta.setDisplayName("§e§l← Vorherige Seite");
            prevMeta.setLore(Arrays.asList(
                    "§7Seite " + currentPage + "/" + totalPages,
                    "",
                    "§7Klicke um zurückzublättern"
            ));
            prevPage.setItemMeta(prevMeta);
            inv.setItem(45, prevPage);
        } else {
            // Platzhalter wenn keine vorherige Seite
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta placeholderMeta = placeholder.getItemMeta();
            placeholderMeta.setDisplayName("§8");
            placeholder.setItemMeta(placeholderMeta);
            inv.setItem(45, placeholder);
        }

        // Info-Item (Slot 49)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lChallenge Info");

        List<String> lore = new ArrayList<>();
        lore.add("§7Einzelkämpfer: §e" + cm.getEinzelkaempferAchievementCount() + " Achievements");
        lore.add("§7Alle anderen: §e" + cm.getOthersAchievementCount() + " verschiedene Achievements");
        lore.add("");
        lore.add("§7Gesamt: §e" + totalAchievements + " Achievements");
        lore.add("§7Seite: §e" + (currentPage + 1) + "/" + totalPages);

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        inv.setItem(49, info);

        // Nächste Seite (Slot 53)
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            nextMeta.setDisplayName("§e§lNächste Seite →");
            nextMeta.setLore(Arrays.asList(
                    "§7Seite " + (currentPage + 2) + "/" + totalPages,
                    "",
                    "§7Klicke um weiterzublättern"
            ));
            nextPage.setItemMeta(nextMeta);
            inv.setItem(53, nextPage);
        } else {
            // Platzhalter wenn keine nächste Seite
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta placeholderMeta = placeholder.getItemMeta();
            placeholderMeta.setDisplayName("§8");
            placeholder.setItemMeta(placeholderMeta);
            inv.setItem(53, placeholder);
        }

        // Filter-Wechsel-Button (Slot 48) - verschoben von 53
        ItemStack filterButton = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filterButton.getItemMeta();
        filterMeta.setDisplayName("§6§lAnsicht wechseln");
        filterMeta.setLore(Arrays.asList(
                "§7Aktuell: §e" + ViewMode.ACHIEVEMENTS.getDisplayName(),
                "",
                "§7Klicke um zur",
                "§e" + ViewMode.PLAYERS.getDisplayName() + " §7zu wechseln"
        ));
        filterButton.setItemMeta(filterMeta);
        inv.setItem(48, filterButton);

        player.openInventory(inv);
    }

    /**
     * Erstellt ein Achievement-Status-Item mit allen Infos
     */
    private ItemStack createAchievementStatusItem(String achievementKey, ChallengeManager cm) {
        de.achievementchallenge.utils.AchievementRegistry.AchievementInfo info =
                de.achievementchallenge.utils.AchievementRegistry.getAchievementInfo(achievementKey);

        ItemStack item = new ItemStack(info.getIcon());
        ItemMeta meta = item.getItemMeta();

        // Prüfe ob Einzelkämpfer dieses Achievement hat
        UUID einzelkaempferUUID = cm.getEinzelkaempferUUID();
        boolean einzelkaempferHasIt = plugin.getAchievementProgressManager()
                .hasCompleted(einzelkaempferUUID, achievementKey);

        // Prüfe ob jemand von den anderen dieses Achievement hat
        Set<String> othersAchievements = cm.getOthersAchievements();
        boolean othersHaveIt = othersAchievements.contains(achievementKey);

        // Hole First Completion Daten
        de.achievementchallenge.managers.AchievementProgressManager.AchievementData firstData =
                plugin.getAchievementProgressManager().getFirstCompletion(achievementKey);

        // Titel-Farbe basierend auf Status
        String titleColor = "§7"; // Grau = niemand hat es
        if (einzelkaempferHasIt && othersHaveIt) {
            titleColor = "§e"; // Gelb = beide haben es
        } else if (einzelkaempferHasIt) {
            titleColor = "§a"; // Grün = nur Einzelkämpfer
        } else if (othersHaveIt) {
            titleColor = "§c"; // Rot = nur andere
        }

        meta.setDisplayName(titleColor + "§l" + info.getTitle());

        // Lore erstellen
        List<String> lore = new ArrayList<>();
        lore.add("§7" + info.getDescription());
        lore.add("");

        // Status für Einzelkämpfer
        if (einzelkaempferHasIt) {
            lore.add("§a✓ Einzelkämpfer: §e" + cm.getEinzelkaempferName());
        } else {
            lore.add("§c✗ Einzelkämpfer: §7Nicht erreicht");
        }

        // Status für andere
        if (othersHaveIt) {
            lore.add("§a✓ Andere Spieler: §eJa");
        } else {
            lore.add("§c✗ Andere Spieler: §7Nein");
        }

        lore.add("");

        // First Completion Info
        if (firstData != null) {
            lore.add("§6§lFirst Completion:");
            lore.add("§7Spieler: §e" + firstData.getFirstCompleterName());

            // Zeit formatieren
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            lore.add("§7Zeit: §f" + sdf.format(new Date(firstData.getFirstCompletionTime())));
            lore.add("§7Timer: §6" + de.achievementchallenge.managers.DataManager.formatTime(firstData.getTimerTicksAtCompletion()));
        } else {
            lore.add("§7Noch von niemandem erreicht!");
        }

        lore.add("");
        lore.add("§7Kategorie: §e" + info.getCategory().getDisplayName());

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * Erstellt einen Spielerkopf mit Achievement-Info
     *
     * @param offlinePlayer Der Spieler
     * @param displayName Anzeigename
     * @param achievementCount Anzahl der Achievements
     * @return ItemStack mit Spielerkopf
     */
    private ItemStack createPlayerHead(OfflinePlayer offlinePlayer, String displayName, int achievementCount) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("§7Achievements: §e" + achievementCount);
        meta.setLore(lore);

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Wechselt den View-Modus für einen Spieler
     * WICHTIG: Diese Methode wird vom ChallengeInventoryClickListener aufgerufen!
     */
    public void toggleViewMode(Player player) {
        ViewMode current = playerViewMode.getOrDefault(player.getUniqueId(), ViewMode.PLAYERS);
        ViewMode next = (current == ViewMode.PLAYERS) ? ViewMode.ACHIEVEMENTS : ViewMode.PLAYERS;
        playerViewMode.put(player.getUniqueId(), next);

        // Setze Seite auf 0 wenn zu Achievements gewechselt wird
        if (next == ViewMode.ACHIEVEMENTS) {
            playerCurrentPage.put(player.getUniqueId(), 0);
        }
    }

    /**
     * Geht zur nächsten Seite
     */
    public void nextPage(Player player) {
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        playerCurrentPage.put(player.getUniqueId(), currentPage + 1);
    }

    /**
     * Geht zur vorherigen Seite
     */
    public void previousPage(Player player) {
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        if (currentPage > 0) {
            playerCurrentPage.put(player.getUniqueId(), currentPage - 1);
        }
    }

    /**
     * Gibt den aktuellen ViewMode eines Spielers zurück
     */
    public ViewMode getViewMode(UUID playerId) {
        return playerViewMode.getOrDefault(playerId, ViewMode.PLAYERS);
    }
}