package ru.obsidianspire.sanity.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class WipeEventManager {
    private final ObsidianSpireSanity plugin;
    private boolean isWipeEventActive = false;
    private World whiteRoomWorld;

    public WipeEventManager(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
        this.isWipeEventActive = plugin.getDataProvider().loadWipeStatus();

        if (this.isWipeEventActive) {
            setupWhiteRoom();
        }
    }

    public boolean isWipeEventActive() {
        return isWipeEventActive;
    }

    public void startWipeEvent() {
        if (isWipeEventActive) return;

        isWipeEventActive = true;
        plugin.getDataProvider().saveWipeStatus(true);
        setupWhiteRoom();

        // Start sequence for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playWipeSequence(player);
        }
    }

    private void playWipeSequence(Player player) {
        // Darkness and effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 4, false, false, false));

        // Play music disc 13
        player.playSound(player.getLocation(), org.bukkit.Sound.MUSIC_DISC_13, org.bukkit.SoundCategory.RECORDS, 1.0f, 1.0f);

        player.sendMessage(ChatColor.GRAY + "Доброе утро, сони. Время просыпаться.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.kickPlayer(ChatColor.WHITE + "Вы открываете глаза. Стены вокруг мягкие и белые. Эксперимент окончен. Вы дома.");
                }
            }
        }.runTaskLater(plugin, 100L); // Kick after 5 seconds
    }

    public void handlePlayerJoin(Player player) {
        if (!isWipeEventActive) return;

        if (whiteRoomWorld == null) {
            setupWhiteRoom();
        }

        // Delay teleport slightly so the player finishes joining
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.setBedSpawnLocation(whiteRoomWorld.getSpawnLocation(), true);
                    player.teleport(whiteRoomWorld.getSpawnLocation());
                    player.setGameMode(GameMode.ADVENTURE);
                    player.getInventory().clear();
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);

                    // Add permanent blindness or other effects if desired, but lore says "empty white room"
                    // Let's remove effects that might have carried over
                    for (PotionEffect effect : player.getActivePotionEffects()) {
                        player.removePotionEffect(effect.getType());
                    }
                }
            }
        }.runTaskLater(plugin, 10L);
    }

    private void setupWhiteRoom() {
        String worldName = plugin.getConfigManager().whiteRoomWorldName;
        if (Bukkit.getWorld(worldName) == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.type(WorldType.FLAT);
            creator.generatorSettings("{\"structures\": {\"structures\": {}}, \"layers\": [{\"block\": \"minecraft:white_concrete\", \"height\": 64}], \"biome\": \"minecraft:the_void\"}");
            creator.generateStructures(false);
            whiteRoomWorld = Bukkit.createWorld(creator);
            if (whiteRoomWorld != null) {
                // Adjust world rules
                whiteRoomWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                whiteRoomWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                whiteRoomWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
                whiteRoomWorld.setTime(6000); // Noon

                // Set spawn point
                whiteRoomWorld.setSpawnLocation(0, 65, 0);
            }
        } else {
            whiteRoomWorld = Bukkit.getWorld(worldName);
        }
    }
}
