package ru.obsidianspire.sanity.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class HallucinationCommand implements CommandExecutor {
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
}
