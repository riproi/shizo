package ru.obsidianspire.sanity.hallucination;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class Phantom1PursuitHallucination extends Hallucination {

    private Zombie phantomEntity;
    private ArmorStand cameraEntity;
    private int taskID = -1;

    public Phantom1PursuitHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline() || player.isDead()) return;

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(-5)); // 5 blocks behind

        phantomEntity = (Zombie) player.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        phantomEntity.setCustomNameVisible(false);
        phantomEntity.setInvulnerable(true);
        phantomEntity.setSilent(true);
        phantomEntity.getEquipment().clear();

        org.bukkit.attribute.AttributeInstance damageAttr = phantomEntity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(0.0);
        }

        // Hide from everyone except the player
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

        taskID = new BukkitRunnable() {
            int ticksAlive = 0;
            int stareTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || phantomEntity.isDead() || ticksAlive > 1200) {
                    stop();
                    cancel();
                    return;
                }

                phantomEntity.setTarget(player);

                // Dynamic speed (slightly faster than player)
                double playerSpeed = player.getVelocity().length();
                if (playerSpeed > 0.1) {
                    phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 3));
                } else {
                    phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
                }

                double distance = phantomEntity.getLocation().distance(player.getLocation());

                // Check if player is looking at Phantom
                if (distance <= 20) {
                    Location eye = player.getEyeLocation();
                    org.bukkit.util.Vector toEntity = phantomEntity.getLocation().toVector().subtract(eye.toVector());
                    double dot = toEntity.normalize().dot(eye.getDirection());

                    if (dot > 0.95) { // Looking almost directly at
                        stareTicks += 5;
                        if (stareTicks >= 20) { // 1 second
                            // Trigger effects
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 2)); // Blindness III
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3)); // Slowness IV

                            // Play sound twice
                            player.playSound(player.getLocation(), Sound.MUSIC_DISC_13, 1.0f, 0.5f);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (player.isOnline()) {
                                        player.playSound(player.getLocation(), Sound.MUSIC_DISC_13, 1.0f, 0.5f);
                                    }
                                }
                            }.runTaskLater(plugin, 40L); // Play again after 2 seconds

                            player.sendTitle(plugin.getConfigManager().textPhantom1Pursuit, "", 10, 100, 10);

                            ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
                            if (data != null && !data.isAminazineActive() && !data.isClozapineActive()) {
                                data.removeSanity(10.0);
                            }

                            stop();
                            cancel();
                            return;
                        }
                    } else {
                        stareTicks = 0;
                    }
                }

                // If too far, teleport behind and force look
                if (distance > 30 && cameraEntity == null) {
                    Location behind = player.getLocation().add(player.getLocation().getDirection().multiply(-2));
                    phantomEntity.teleport(behind);

                    // Smooth turn to phantom
                    triggerSmoothTurn(phantomEntity.getLocation());
                }

                ticksAlive += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L).getTaskId();
    }

    private void triggerSmoothTurn(Location targetLocEntity) {
        Location startLoc = player.getEyeLocation();

        cameraEntity = (ArmorStand) player.getWorld().spawnEntity(startLoc, EntityType.ARMOR_STAND);
        cameraEntity.setInvisible(true);
        cameraEntity.setGravity(false);
        cameraEntity.setInvulnerable(true);
        cameraEntity.setMarker(true);

        org.bukkit.util.Vector toEntity = targetLocEntity.toVector().subtract(startLoc.toVector());
        float targetYaw = (float) Math.toDegrees(Math.atan2(-toEntity.getX(), toEntity.getZ()));
        float targetPitch = (float) Math.toDegrees(Math.asin(-toEntity.getY() / toEntity.length()));

        PacketContainer cameraPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
        cameraPacket.getIntegers().write(0, cameraEntity.getEntityId());
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, cameraPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    restoreCamera();
                    cancel();
                    return;
                }

                float progress = (float) ticks / maxTicks;
                float currentYaw = startLoc.getYaw() + (targetYaw - startLoc.getYaw()) * progress;
                float currentPitch = startLoc.getPitch() + (targetPitch - startLoc.getPitch()) * progress;

                Location loc = cameraEntity.getLocation();
                loc.setYaw(currentYaw);
                loc.setPitch(currentPitch);
                cameraEntity.teleport(loc);

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void restoreCamera() {
        if (player.isOnline()) {
            PacketContainer restoreCamera = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
            restoreCamera.getIntegers().write(0, player.getEntityId());
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, restoreCamera);

                if (cameraEntity != null) {
                    Location finalLoc = cameraEntity.getLocation();
                    Location pLoc = player.getLocation();
                    pLoc.setYaw(finalLoc.getYaw());
                    pLoc.setPitch(finalLoc.getPitch());
                    player.teleport(pLoc);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (cameraEntity != null) {
            cameraEntity.remove();
            cameraEntity = null;
        }
    }

    @Override
    public void stop() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }

        restoreCamera();

        if (phantomEntity != null) {
            phantomEntity.remove();
            phantomEntity = null;
        }
    }
}
