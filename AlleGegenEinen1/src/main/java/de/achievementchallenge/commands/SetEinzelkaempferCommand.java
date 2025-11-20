package de.achievementchallenge.commands;

import de.achievementchallenge.AchievementChallengePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Command: /seteinzelkaempfer <spieler>
 *
 * Setzt einen Spieler als Einzelkämpfer für die Challenge.
 * Zeigt eine Bestätigungs-GUI an, bevor die Challenge gestartet wird.
 *
 * Nach Bestätigung:
 * - Löscht alle Achievements und Inventare
 * - Öffnet GUI zur Zeitfestlegung
 * - Startet Timer bei 0
 */
public class SetEinzelkaempferCommand implements CommandExecutor {

    private final AchievementChallengePlugin plugin;

    public SetEinzelkaempferCommand(AchievementChallengePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Prüfe ob Sender ein Spieler ist
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Command kann nur von Spielern ausgeführt werden!");
            return true;
        }

        Player player = (Player) sender;

        // Prüfe OP-Rechte
        if (!player.isOp()) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Command!");
            return true;
        }

        // Prüfe Argumente
        if (args.length != 1) {
            player.sendMessage("§cFalsche Verwendung!");
            player.sendMessage("§7Verwendung: /seteinzelkaempfer <spieler>");
            return true;
        }

        // Suche Zielspieler
        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            player.sendMessage("§cSpieler nicht gefunden: " + args[0]);
            return true;
        }

        // Öffne Bestätigungs-GUI
        openConfirmationGUI(player, target);

        return true;
    }

    /**
     * Öffnet die Bestätigungs-GUI
     *
     * @param executor Der Spieler, der den Command ausführt
     * @param target Der Spieler, der Einzelkämpfer werden soll
     */
    private void openConfirmationGUI(Player executor, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, "§c§lEinzelkämpfer setzen?");

        // Bestätigen-Item
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lBestätigen");
        confirmMeta.setLore(Arrays.asList(
                "§7Setze §e" + target.getName() + " §7als Einzelkämpfer",
                "",
                "§c§lWARNUNG:",
                "§7- Alle Achievements werden entfernt",
                "§7- Alle Inventare werden geleert",
                "§7- Eine laufende Challenge wird abgebrochen"
        ));
        confirm.setItemMeta(confirmMeta);

        // Abbrechen-Item
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lAbbrechen");
        cancelMeta.setLore(Arrays.asList("§7Klicke zum Abbrechen"));
        cancel.setItemMeta(cancelMeta);

        // Ziel-Spieler Kopf (zur Anzeige)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta headMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(target);
        headMeta.setDisplayName("§e" + target.getName());
        headMeta.setLore(Arrays.asList(
                "§7Wird zum §c§lEinzelkämpfer§7!",
                "",
                "§7Ziel: Mehr Achievements schaffen",
                "§7als alle anderen zusammen!"
        ));
        head.setItemMeta(headMeta);

        // Items platzieren
        inv.setItem(11, confirm);
        inv.setItem(13, head);
        inv.setItem(15, cancel);

        executor.openInventory(inv);
    }
}