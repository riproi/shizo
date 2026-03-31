package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import org.bukkit.Sound;

public class PhantomHungerHallucination extends Hallucination {
    private int taskID = -1;

    public PhantomHungerHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null) return;

        // Save original food to restore it later
        data.setPhantomHungerOriginalFood(player.getFoodLevel());
        data.setPhantomHungerOriginalSat(player.getSaturation());

        // Drop to 6 to show missing hunger bars, allowing them to try to eat.
        player.setFoodLevel(6);
        player.setSaturation(0);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 0.5f, 1.0f);

        taskID = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 300) { // 15 seconds max if they don't eat
                    stop();
                    cancel();
                    return;
                }

                ticks += 20; // 1 second
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    @Override
    public void stop() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }

        if (player.isOnline()) {
            ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
            if (data != null && data.getPhantomHungerOriginalFood() != -1) {
                player.setFoodLevel(data.getPhantomHungerOriginalFood());
                player.setSaturation(data.getPhantomHungerOriginalSat());
                data.setPhantomHungerOriginalFood(-1);
            } else {
                player.setFoodLevel(20);
                player.setSaturation(5.0f);
            }
        }
    }
}
