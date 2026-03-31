package ru.obsidianspire.sanity.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.data.PlayerData;

import java.util.UUID;

public class SanityManager {
    private final ObsidianSpireSanity plugin;

    public SanityManager(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
    }

    public enum SanityStage {
        NORMAL(90, 100),
        ANXIETY(70, 89.99),
        PARANOIA(50, 69.99),
        PSYCHOSIS(30, 49.99),
        COLLAPSE(0, 29.99);

        private final double min;
        private final double max;

        SanityStage(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public static SanityStage getStage(double sanity) {
            if (sanity >= NORMAL.min) return NORMAL;
            if (sanity >= ANXIETY.min) return ANXIETY;
            if (sanity >= PARANOIA.min) return PARANOIA;
            if (sanity >= PSYCHOSIS.min) return PSYCHOSIS;
            return COLLAPSE;
        }
    }

    public void processSanityCycle() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getPlayerData(player.getUniqueId());
            if (data == null) continue;

            // Check if player is protected by Aminazine
            if (data.isAminazineActive() || data.isClozapineActive()) {
                continue;
            }

            double sanityChange = 0.0;

            // 1. Darkness: Light level below 7 (-1% per minute)
            if (player.getLocation().getBlock().getLightLevel() < 7) {
                sanityChange -= plugin.getConfigManager().sanityDropDarkness;
            }

            // 2. Isolation: No players in 50 blocks for 10 minutes (-5% per cycle)
            boolean isIsolated = true;
            for (Player other : player.getWorld().getPlayers()) {
                if (other.equals(player)) continue;
                if (other.getLocation().distance(player.getLocation()) <= 50) {
                    isIsolated = false;
                    break;
                }
            }

            if (isIsolated) {
                long isolatedTime = System.currentTimeMillis() - data.getLastTimeIsolated();
                if (isolatedTime >= 10 * 60 * 1000) { // 10 minutes
                    sanityChange -= plugin.getConfigManager().sanityDropIsolation;
                    data.setLastTimeIsolated(System.currentTimeMillis()); // Reset timer after hit
                }
            } else {
                data.setLastTimeIsolated(System.currentTimeMillis()); // Reset timer if not isolated
            }

            // Group Therapy: Near player with >80% sanity (+5% per minute)
            boolean groupTherapy = false;
            for (Player other : player.getWorld().getPlayers()) {
                if (other.equals(player)) continue;
                if (other.getLocation().distance(player.getLocation()) <= 20) {
                    PlayerData otherData = plugin.getPlayerData(other.getUniqueId());
                    if (otherData != null && otherData.getSanity() > 80.0) {
                        groupTherapy = true;
                        break;
                    }
                }
            }

            if (groupTherapy) {
                sanityChange += plugin.getConfigManager().sanityHealGroupTherapy;
            }

            if (sanityChange != 0.0) {
                data.addSanity(sanityChange);
            }
        }
    }

    public void addSanity(Player player, double amount) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.addSanity(amount);
        }
    }

    public void removeSanity(Player player, double amount) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null) {
            // Check protections
            if (!data.isAminazineActive() && !data.isClozapineActive()) {
                data.removeSanity(amount);
            }
        }
    }

    public SanityStage getStage(Player player) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null) return SanityStage.NORMAL;
        return SanityStage.getStage(data.getSanity());
    }
}
