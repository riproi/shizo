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
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MedicineCommand implements CommandExecutor, TabCompleter {
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("obsidianspire.sanity.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("give", "stats").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                return Arrays.asList("aminazine", "haloperidol", "clozapine", "tea").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("stats")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
