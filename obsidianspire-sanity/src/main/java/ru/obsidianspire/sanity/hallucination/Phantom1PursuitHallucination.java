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
import org.bukkit.metadata.FixedMetadataValue;
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
        phantomEntity.setMetadata("hallucination_phantom", new FixedMetadataValue(plugin, true));

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
            int unseenTicks = 0;
            boolean isLockingSequence = false;

            @Override
            public void run() {
                if (!player.isOnline() || phantomEntity.isDead() || ticksAlive > 1200) {
                    stop();
                    cancel();
                    return;
                }

                if (isLockingSequence) {
                    return; // AI stops processing during lock
                }

                phantomEntity.setTarget(player);

                // Dynamic speed
                double playerSpeed = player.getVelocity().length();
                if (playerSpeed > 0.25) {
                    phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 5));
                } else if (playerSpeed > 0.15) {
                    phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 4));
                } else if (playerSpeed > 0.05) {
                    phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 3));
                } else {
                    phantomEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1));
                }

                double distance = phantomEntity.getLocation().distance(player.getLocation());
                boolean hasLineOfSight = player.hasLineOfSight(phantomEntity);

                if (!hasLineOfSight) {
                    unseenTicks += 5;
                } else {
                    unseenTicks = 0;
                }

                // Check if player is looking at Phantom
                if (distance <= 20 && hasLineOfSight) {
                    Location eye = player.getEyeLocation();
                    org.bukkit.util.Vector toEntity = phantomEntity.getLocation().toVector().subtract(eye.toVector());
                    double dot = toEntity.normalize().dot(eye.getDirection());

                    if (dot > 0.95) { // Looking almost directly at
                        stareTicks += 5;
                        if (stareTicks >= 20) { // 1 second
                            isLockingSequence = true;
                            triggerLockSequence(false);
                            return;
                        }
                    } else {
                        stareTicks = 0;
                    }
                }

                // If unseen for 4 seconds (80 ticks) or too far, teleport behind and force look
                if ((unseenTicks >= 80 || distance > 30) && cameraEntity == null) {
                    isLockingSequence = true;
                    Location behind = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(-2));
                    phantomEntity.teleport(behind);

                    triggerLockSequence(true);
                    return;
                }

                ticksAlive += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L).getTaskId();
    }

    private void triggerLockSequence(boolean needsSmoothTurn) {
        phantomEntity.setAI(false);
        player.playSound(player.getLocation(), Sound.MUSIC_DISC_13, 1.0f, 0.5f);

        // Schedule stop sound in 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.stopSound(Sound.MUSIC_DISC_13);
                }
            }
        }.runTaskLater(plugin, 200L); // 10 seconds

        if (needsSmoothTurn) {
            triggerSmoothTurnAndLock(phantomEntity.getLocation());
        } else {
            lockAndFinish();
        }
    }

    private void lockAndFinish() {
        // Just lock for 1 second where the player is currently looking
        triggerSmoothTurn(player.getEyeLocation().add(player.getLocation().getDirection().multiply(2)), 20, this::finishSequence);
    }

    private void triggerSmoothTurnAndLock(Location targetLocEntity) {
        // Smooth turn takes 1 second, then locks for 1 second before finishing
        triggerSmoothTurn(targetLocEntity, 20, () -> {
            // Lock for 1 second by holding camera
            triggerSmoothTurn(targetLocEntity, 20, this::finishSequence);
        });
    }

    private void finishSequence() {
        if (!player.isOnline()) return;

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 2)); // Blindness III
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3)); // Slowness IV
        player.sendTitle(plugin.getConfigManager().textPhantom1Pursuit, "", 10, 100, 10);

        ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null && !data.isAminazineActive() && !data.isClozapineActive()) {
            data.removeSanity(10.0);
        }

        // The code reviewer mentioned `restoreCamera` is skipped if `onComplete` is provided.
        // We provided `this::finishSequence` as `onComplete`. So we must ensure camera is restored.
        // Calling `stop()` invokes `restoreCamera()`. So we are safe, but let's make it explicitly clear.
        restoreCamera();
        stop();
    }

    private void triggerSmoothTurn(Location targetLocEntity, int maxTicks, Runnable onComplete) {
        Location startLoc = player.getEyeLocation();

        // If a camera entity already exists (e.g. chained lock sequence), use its location as start
        if (cameraEntity != null) {
            startLoc = cameraEntity.getLocation();
        } else {
            cameraEntity = (ArmorStand) player.getWorld().spawnEntity(startLoc, EntityType.ARMOR_STAND);
            cameraEntity.setInvisible(true);
            cameraEntity.setGravity(false);
            cameraEntity.setInvulnerable(true);
            cameraEntity.setMarker(true);

            PacketContainer cameraPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
            cameraPacket.getIntegers().write(0, cameraEntity.getEntityId());
            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, cameraPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        org.bukkit.util.Vector toEntity = targetLocEntity.toVector().subtract(startLoc.toVector());
        float targetYaw = (float) Math.toDegrees(Math.atan2(-toEntity.getX(), toEntity.getZ()));
        float targetPitch = (float) Math.toDegrees(Math.asin(-toEntity.getY() / toEntity.length()));

        final Location initialLoc = startLoc;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    if (onComplete != null && player.isOnline()) {
                        onComplete.run();
                    } else {
                        restoreCamera();
                    }
                    cancel();
                    return;
                }

                float progress = (float) ticks / maxTicks;
                float currentYaw = initialLoc.getYaw() + (targetYaw - initialLoc.getYaw()) * progress;
                float currentPitch = initialLoc.getPitch() + (targetPitch - initialLoc.getPitch()) * progress;

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
