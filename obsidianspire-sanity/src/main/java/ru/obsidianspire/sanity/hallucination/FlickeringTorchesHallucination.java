package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlickeringTorchesHallucination extends Hallucination {
    private int taskID = -1;
    private final Random rand = new Random();

    public FlickeringTorchesHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        taskID = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > 400) { // 20s
                    stop();
                    cancel();
                    return;
                }

                Location pLoc = player.getLocation();
                List<Block> torches = new ArrayList<>();
                for (int x = -10; x <= 10; x++) {
                    for (int y = -10; y <= 10; y++) {
                        for (int z = -10; z <= 10; z++) {
                            Block b = pLoc.clone().add(x, y, z).getBlock();
                            if (b.getType() == Material.TORCH || b.getType() == Material.WALL_TORCH) {
                                torches.add(b);
                            }
                        }
                    }
                }

                if (!torches.isEmpty()) {
                    Block randomTorch = torches.get(rand.nextInt(torches.size()));

                    // Send block change packet to hide it
                    Material oldType = randomTorch.getType();
                    Material newType = Material.AIR;

                    player.sendBlockChange(randomTorch.getLocation(), newType.createBlockData());

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.sendBlockChange(randomTorch.getLocation(), oldType.createBlockData());
                            }
                        }
                    }.runTaskLater(plugin, 10L); // revert after 0.5s
                }

                ticks += 20; // 1s
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    @Override
    public void stop() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
