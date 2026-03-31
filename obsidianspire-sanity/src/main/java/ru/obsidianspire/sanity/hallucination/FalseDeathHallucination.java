package ru.obsidianspire.sanity.hallucination;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class FalseDeathHallucination extends Hallucination {

    public FalseDeathHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline() || player.isDead()) return;

        try {
            // Trigger Death Screen cleanly: UPDATE_HEALTH to 0
            PacketContainer healthPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_HEALTH);
            healthPacket.getFloat().write(0, 0.0f); // Health
            healthPacket.getIntegers().write(0, player.getFoodLevel()); // Food
            healthPacket.getFloat().write(1, player.getSaturation()); // Saturation
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, healthPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Restore after 2 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && !player.isDead()) {
                    try {
                        // Restore actual health to remove dead status
                        PacketContainer restorePacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.UPDATE_HEALTH);
                        restorePacket.getFloat().write(0, (float) player.getHealth());
                        restorePacket.getIntegers().write(0, player.getFoodLevel());
                        restorePacket.getFloat().write(1, player.getSaturation());
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, restorePacket);

                        // Since UPDATE_HEALTH > 0 doesn't automatically close the "You Died" GUI on modern clients,
                        // and teleportation can sometimes leave it open or cause ghost items,
                        // we must force the client to resync state cleanly without full dimension registry hacking.

                        // Force full server respawn sequence on the Bukkit side to completely resync the client
                        // This uses Spigot's respawn() API which safely handles the complex packet sequence for us.
                        // Spigot allows calling respawn() even when alive to trigger the respawn sequence for the client.
                        // However, just calling it when alive might throw an error or do nothing.
                        // So the absolute safest cross-version method to force close GUIs and resync is:

                        // 1. Send RESPAWN packet
                        // We avoid manual packet construction because ProtocolLib 1.21 respawn packets are very complex.
                        // Instead, we use Paper/Purpur's built-in player.spigot().respawn() or teleportation.
                        // Teleporting the player to another world and back is the guaranteed way to send a respawn packet.
                        org.bukkit.Location originalLoc = player.getLocation();
                        org.bukkit.World otherWorld = org.bukkit.Bukkit.getWorlds().stream()
                            .filter(w -> !w.equals(originalLoc.getWorld())).findFirst().orElse(null);

                        if (otherWorld != null) {
                            // Teleport to other world, then immediately back to send clean respawn packets
                            player.teleport(otherWorld.getSpawnLocation());
                            player.teleport(originalLoc);
                        } else {
                            // Fallback if only 1 world exists (e.g. flat test server)
                            // A large distance teleport triggers a chunk reload/resync
                            org.bukkit.Location farLoc = originalLoc.clone().add(0, 500, 0);
                            player.teleport(farLoc);
                            player.teleport(originalLoc);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.runTaskLater(plugin, 40L);
    }

    @Override
    public void stop() {
        // Handled automatically by the task
    }
}
