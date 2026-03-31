package ru.obsidianspire.sanity.hallucination;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class EyesInTheCrowdHallucination extends Hallucination {

    public EyesInTheCrowdHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        List<LivingEntity> crowd = new ArrayList<>();
        for (Entity e : player.getNearbyEntities(10, 5, 10)) {
            if (e instanceof LivingEntity && e != player) {
                crowd.add((LivingEntity) e);
            }
        }

        if (crowd.size() >= 3) {
            for (LivingEntity entity : crowd) {
                // Look at player (Packet PlayOutEntityHeadRotation and EntityLook)
                Location eye = player.getEyeLocation();
                Location entLoc = entity.getLocation();
                org.bukkit.util.Vector toPlayer = eye.toVector().subtract(entLoc.toVector());
                float yaw = (float) Math.toDegrees(Math.atan2(-toPlayer.getX(), toPlayer.getZ()));
                float pitch = (float) Math.toDegrees(Math.asin(-toPlayer.getY() / toPlayer.length()));

                // Send head rotation packet
                PacketContainer headPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                headPacket.getIntegers().write(0, entity.getEntityId());
                headPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));

                // Send body rotation packet
                PacketContainer lookPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_LOOK);
                lookPacket.getIntegers().write(0, entity.getEntityId());
                lookPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
                lookPacket.getBytes().write(1, (byte) (pitch * 256.0F / 360.0F));
                lookPacket.getBooleans().write(0, true);

                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, headPacket);
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, lookPacket);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Restore rotation after short time
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline() && !entity.isDead()) {
                            // Restore using their actual current rotation
                            float oldYaw = entity.getLocation().getYaw();
                            float oldPitch = entity.getLocation().getPitch();

                            PacketContainer rHeadPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
                            rHeadPacket.getIntegers().write(0, entity.getEntityId());
                            rHeadPacket.getBytes().write(0, (byte) (oldYaw * 256.0F / 360.0F));

                            PacketContainer rLookPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_LOOK);
                            rLookPacket.getIntegers().write(0, entity.getEntityId());
                            rLookPacket.getBytes().write(0, (byte) (oldYaw * 256.0F / 360.0F));
                            rLookPacket.getBytes().write(1, (byte) (oldPitch * 256.0F / 360.0F));
                            rLookPacket.getBooleans().write(0, true);

                            try {
                                ProtocolLibrary.getProtocolManager().sendServerPacket(player, rHeadPacket);
                                ProtocolLibrary.getProtocolManager().sendServerPacket(player, rLookPacket);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }.runTaskLater(plugin, 10L); // 0.5 sec
            }
        }
    }

    @Override
    public void stop() {
        // Handled via task
    }
}
