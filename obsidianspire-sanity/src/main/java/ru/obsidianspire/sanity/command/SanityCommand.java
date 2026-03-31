package ru.obsidianspire.sanity.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.data.PlayerData;
import ru.obsidianspire.sanity.manager.SanityManager;
import org.bukkit.command.TabCompleter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class SanityCommand implements CommandExecutor, TabCompleter {
    private final ObsidianSpireSanity plugin;
    private final Map<UUID, Long> wipeConfirmations = new HashMap<>();

    public SanityCommand(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obsidianspire.sanity.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /sanity <set|add|remove|get|wipe_event> <player> [value]");
            return true;
        }

        String action = args[0].toLowerCase();

        // Wipe event handling
        if (action.equals("wipe_event")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /sanity wipe_event <start|confirm>");
                return true;
            }
            if (args[1].equalsIgnoreCase("start")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    wipeConfirmations.put(p.getUniqueId(), System.currentTimeMillis());
                    p.sendMessage(ChatColor.YELLOW + "Warning: This will start the wipe event! Type '/sanity wipe_event confirm' within 10 seconds to confirm.");
                } else {
                    plugin.getWipeEventManager().startWipeEvent();
                    sender.sendMessage(ChatColor.GREEN + "Wipe event started.");
                }
                return true;
            } else if (args[1].equalsIgnoreCase("confirm")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    if (wipeConfirmations.containsKey(p.getUniqueId())) {
                        if (System.currentTimeMillis() - wipeConfirmations.get(p.getUniqueId()) <= 10000) {
                            plugin.getWipeEventManager().startWipeEvent();
                            p.sendMessage(ChatColor.GREEN + "Wipe event started.");
                        } else {
                            p.sendMessage(ChatColor.RED + "Confirmation expired. Run '/sanity wipe_event start' again.");
                        }
                        wipeConfirmations.remove(p.getUniqueId());
                    } else {
                        p.sendMessage(ChatColor.RED + "Nothing to confirm.");
                    }
                }
                return true;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sanity <set|add|remove|get> <player> [value]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        PlayerData data = plugin.getPlayerData(target.getUniqueId());

        if (action.equals("get")) {
            SanityManager.SanityStage stage = plugin.getSanityManager().getStage(target);
            sender.sendMessage(ChatColor.AQUA + target.getName() + "'s Sanity: " + String.format("%.2f%%", data.getSanity()) + " (" + stage.name() + ")");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "You must specify a value.");
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number format.");
            return true;
        }

        switch (action) {
            case "set":
                data.setSanity(value);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s sanity to " + value + "%.");
                break;
            case "add":
                data.addSanity(value);
                sender.sendMessage(ChatColor.GREEN + "Added " + value + "% to " + target.getName() + "'s sanity.");
                break;
            case "remove":
                data.removeSanity(value);
                sender.sendMessage(ChatColor.GREEN + "Removed " + value + "% from " + target.getName() + "'s sanity.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown action.");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("obsidianspire.sanity.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("set", "add", "remove", "get", "wipe_event").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("wipe_event")) {
                return Arrays.asList("start", "confirm").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
