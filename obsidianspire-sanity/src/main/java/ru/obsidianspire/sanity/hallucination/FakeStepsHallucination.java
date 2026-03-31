package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class FakeStepsHallucination extends Hallucination {

    private int taskID = -1;

    public FakeStepsHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        taskID = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 60) {
                    stop();
                    cancel();
                    return;
                }

                // Play footstep slightly offset
                player.playSound(player.getLocation(), Sound.BLOCK_GRASS_STEP, 0.5f, 1.0f);
                ticks++;
            }
        }.runTaskTimer(plugin, 10L, 10L).getTaskId(); // Every 0.5s for 30s
    }

    @Override
    public void stop() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
