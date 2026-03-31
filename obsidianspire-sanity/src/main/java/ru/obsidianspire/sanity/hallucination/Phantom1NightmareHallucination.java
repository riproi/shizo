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

public class Phantom1NightmareHallucination extends Hallucination {

    private Zombie phantomEntity;

    public Phantom1NightmareHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline() || player.isDead()) return;

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(1.5)); // Near bed

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

        // Stand for 1 second, then wake player up
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.isSleeping()) {
                    // Wake up
                    player.wakeup(true);

                    // Effects
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0)); // 10s Blindness
                    player.playSound(player.getLocation(), Sound.MUSIC_DISC_13, 1.0f, 0.5f);
                    player.sendTitle(plugin.getConfigManager().textPhantom1Nightmare, "", 10, 200, 10); // 10s title
                }
                stop();
            }
        }.runTaskLater(plugin, 20L); // 1 second
    }

    @Override
    public void stop() {
        if (phantomEntity != null) {
            phantomEntity.remove();
            phantomEntity = null;
        }
    }
}
