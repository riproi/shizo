package ru.obsidianspire.sanity.hallucination;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

import java.util.UUID;

public class Phantom1Hallucination extends Hallucination {

    private Zombie phantomEntity;
    private int taskID = -1;

    public Phantom1Hallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline() || player.isDead()) return;

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(-5)); // 5 blocks behind

        // Spawn Zombie as Phantom1
        phantomEntity = (Zombie) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        phantomEntity.setCustomName("§cТолько не снова...");
        phantomEntity.setCustomNameVisible(true);
        phantomEntity.setInvulnerable(true);
        phantomEntity.setSilent(true); // Custom sounds handled
        phantomEntity.getEquipment().clear();

        // ProtocolLib to hide from everyone except the player
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.equals(player)) {
                PacketContainer destroyPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                destroyPacket.getIntLists().write(0, java.util.Collections.singletonList(phantomEntity.getEntityId()));
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(p, destroyPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Blindness and Music
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
        player.playSound(player.getLocation(), Sound.MUSIC_DISC_13, 1.0f, 1.0f);
        player.sendMessage("§cТолько не снова...");

        // Tracking Logic
        taskID = new BukkitRunnable() {
            int ticksAlive = 0;

            @Override
            public void run() {
                if (!player.isOnline() || phantomEntity.isDead() || ticksAlive > 1200) { // Max 1 minute life
                    stop();
                    cancel();
                    return;
                }

                phantomEntity.setTarget(player);

                // Speed boost
                phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 2)); // Faster than player

                double distance = phantomEntity.getLocation().distance(player.getLocation());

                // Check if player is looking at Phantom
                if (distance <= 20) {
                    Location eye = player.getEyeLocation();
                    org.bukkit.util.Vector toEntity = phantomEntity.getLocation().toVector().subtract(eye.toVector());
                    double dot = toEntity.normalize().dot(eye.getDirection());
                    if (dot > 0.95) { // Looking almost directly at
                        // Hit by Phantom
                        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 2)); // Blindness III
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3)); // Slowness IV
                        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.5f);

                        // Loss of sanity upon contact with phantom1
                        ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
                        if (data != null && !data.isAminazineActive() && !data.isClozapineActive()) {
                            data.removeSanity(10.0);
                        }

                        stop();
                        cancel();
                        return;
                    }
                }

                // If too far, teleport behind
                if (distance > 30) {
                    Location behind = player.getLocation().add(player.getLocation().getDirection().multiply(-2));
                    phantomEntity.teleport(behind);

                    // Force turn player
                    Location pLoc = player.getLocation();
                    pLoc.setDirection(phantomEntity.getLocation().toVector().subtract(pLoc.toVector()));
                    player.teleport(pLoc);
                }

                ticksAlive += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L).getTaskId();
    }

    @Override
    public void stop() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }

        if (phantomEntity != null) {
            phantomEntity.remove();
            phantomEntity = null;
        }
    }
}
