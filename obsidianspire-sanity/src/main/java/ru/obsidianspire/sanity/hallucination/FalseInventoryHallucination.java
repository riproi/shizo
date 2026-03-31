package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class FalseInventoryHallucination extends Hallucination {

    public FalseInventoryHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        // Open a fake empty inventory with same name
        Inventory fakeInv = Bukkit.createInventory(null, 36, "Inventory");
        player.openInventory(fakeInv);

        // This stops upon reopen
    }

    @Override
    public void stop() {
        if (player.isOnline() && player.getOpenInventory().getTitle().equals("Inventory") && player.getOpenInventory().getTopInventory().getSize() == 36) {
            player.closeInventory();
        }
    }
}
