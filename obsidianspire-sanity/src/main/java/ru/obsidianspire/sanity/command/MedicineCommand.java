package ru.obsidianspire.sanity.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.data.PlayerData;
import ru.obsidianspire.sanity.manager.MedicineManager;

public class MedicineCommand implements CommandExecutor {
    private final ObsidianSpireSanity plugin;

    public MedicineCommand(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obsidianspire.sanity.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /meds <give|stats> <type> <player>");
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("stats")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                PlayerData data = plugin.getPlayerData(target.getUniqueId());
                sender.sendMessage(ChatColor.AQUA + "--- Tolerance Stats for " + target.getName() + " ---");
                sender.sendMessage(ChatColor.YELLOW + "Aminazine: " + data.getToleranceAminazine());
                sender.sendMessage(ChatColor.RED + "Haloperidol: " + data.getToleranceHaloperidol());
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "Clozapine: " + data.getToleranceClozapine());
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found.");
            }
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /meds give <type> <player>");
            return true;
        }

        String typeStr = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        MedicineManager.MedicineType type = null;
        switch (typeStr) {
            case "aminazine": type = MedicineManager.MedicineType.AMINAZINE; break;
            case "haloperidol": type = MedicineManager.MedicineType.HALOPERIDOL; break;
            case "clozapine": type = MedicineManager.MedicineType.CLOZAPINE; break;
            case "tea": type = MedicineManager.MedicineType.HERBAL_TEA; break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown medicine type. Use aminazine, haloperidol, clozapine, tea.");
                return true;
        }

        ItemStack item = plugin.getMedicineManager().createMedicine(type);
        target.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "Gave " + type.name() + " to " + target.getName() + ".");

        return true;
    }
}
