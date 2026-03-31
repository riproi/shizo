package ru.obsidianspire.sanity.hallucination;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

import java.util.UUID;

public class HeavyGlanceHallucination extends Hallucination {

    private ArmorStand cameraEntity;
    private int taskID = -1;

    public HeavyGlanceHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (player.isDead() || !player.isOnline()) return;

        Location startLoc = player.getEyeLocation();

        // Spawn invisible ArmorStand at player's head
        cameraEntity = (ArmorStand) player.getWorld().spawnEntity(startLoc, EntityType.ARMOR_STAND);
        cameraEntity.setInvisible(true);
        cameraEntity.setGravity(false);
        cameraEntity.setInvulnerable(true);
        cameraEntity.setMarker(true);

        // Find a dark corner (simplified: just look down and slightly right)
        Location targetLoc = startLoc.clone();
        targetLoc.setPitch(50);
        targetLoc.setYaw(startLoc.getYaw() + 45);

        // Send PacketPlayOutCamera
        PacketContainer cameraPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
        cameraPacket.getIntegers().write(0, cameraEntity.getEntityId());

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, cameraPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Smooth rotation
        taskID = new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40; // 2 seconds

            @Override
            public void run() {
                if (ticks >= maxTicks || !player.isOnline()) {
                    stop();
                    cancel();
                    return;
                }

                float progress = (float) ticks / maxTicks;
                float currentYaw = startLoc.getYaw() + (targetLoc.getYaw() - startLoc.getYaw()) * progress;
                float currentPitch = startLoc.getPitch() + (targetLoc.getPitch() - startLoc.getPitch()) * progress;

                Location loc = cameraEntity.getLocation();
                loc.setYaw(currentYaw);
                loc.setPitch(currentPitch);
                cameraEntity.teleport(loc);

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L).getTaskId();
    }

    @Override
    public void stop() {
        if (taskID != -1) {
            plugin.getServer().getScheduler().cancelTask(taskID);
            taskID = -1;
        }

        if (player.isOnline()) {
            // Restore camera
            PacketContainer restoreCamera = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
            restoreCamera.getIntegers().write(0, player.getEntityId());

            try {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, restoreCamera);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (cameraEntity != null) {
            cameraEntity.remove();
            cameraEntity = null;
        }
    }
}
