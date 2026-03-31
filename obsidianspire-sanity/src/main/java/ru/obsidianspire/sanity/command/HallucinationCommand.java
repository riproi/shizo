package ru.obsidianspire.sanity.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HallucinationCommand implements CommandExecutor, TabCompleter {
    private final ObsidianSpireSanity plugin;

    public HallucinationCommand(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("obsidianspire.sanity.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /gh <trigger|stop> [type] <player>");
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("stop")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                plugin.getHallucinationManager().stopAllFor(target);
                sender.sendMessage(ChatColor.GREEN + "Stopped all hallucinations for " + target.getName() + ".");
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found.");
            }
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /gh trigger <type> <player>");
            return true;
        }

        String type = args[1].toLowerCase();
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        plugin.getHallucinationManager().triggerSpecificHallucination(target, type);
        sender.sendMessage(ChatColor.GREEN + "Triggered hallucination '" + type + "' for " + target.getName() + ".");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("obsidianspire.sanity.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("trigger", "stop").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("trigger")) {
                List<String> types = Arrays.asList(
                    "phantom1", "phantom1_nightmare", "false_death", "heavy_glance",
                    "chest_scream", "fake_steps", "phantom_fire",
                    "panic_explosion", "echo_of_death", "false_inventory",
                    "phantom_hunger", "flickering_torches", "eyes_in_the_crowd"
                );
                return types.stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("stop")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("trigger")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
