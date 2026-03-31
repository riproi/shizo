package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class ChestScreamHallucination extends Hallucination {

    public ChestScreamHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setHallucinationActiveChestScream(true);
        }
    }

    @Override
    public void stop() {
        if (player.isOnline()) {
            ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
            if (data != null) {
                data.setHallucinationActiveChestScream(false);
            }
        }
    }
}
