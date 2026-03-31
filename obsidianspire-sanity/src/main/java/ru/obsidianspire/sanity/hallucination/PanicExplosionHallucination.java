package ru.obsidianspire.sanity.hallucination;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class PanicExplosionHallucination extends Hallucination {

    public PanicExplosionHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        // Visual Explosion Effect
        player.spawnParticle(Particle.EXPLOSION, player.getLocation(), 1);

        // Red Screen Flash -> Can be simulated with a quick damage tint or warning
        PacketContainer warningPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.WORLD_BORDER);
        // Note: Modern packet structure for world border warning is different,
        // using SET_WARNING_BLOCKS or SET_WARNING_DELAY.
        // Another easier way to flash red is a quick tint using vignette.

        // Sending a quick vignette tint by going outside world border
        try {
            // Simplified: Add a brief blindness and nausea, or just play damage animation
            PacketContainer damageAnim = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.HURT_ANIMATION);
            damageAnim.getIntegers().write(0, player.getEntityId());
            damageAnim.getFloat().write(0, 0.0f);
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, damageAnim);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Instant
    }
}
