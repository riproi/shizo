package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class PhantomFireHallucination extends Hallucination {

    public PhantomFireHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline() || player.isDead()) return;

        // Visual fire effect (Fire ticks)
        int originalFireTicks = player.getFireTicks();
        player.setFireTicks(60); // 3 seconds
        player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1.0f, 1.0f);

        // Schedule removal of fire ticks if they haven't actually been set on fire
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (originalFireTicks <= 0) {
                        player.setFireTicks(0);
                    }
                }
            }
        }.runTaskLater(plugin, 60L);
    }

    @Override
    public void stop() {
        if (player.isOnline()) {
            player.setFireTicks(0);
        }
    }
}
