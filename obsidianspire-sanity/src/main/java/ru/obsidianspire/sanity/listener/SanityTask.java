package ru.obsidianspire.sanity.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.manager.SanityManager;

public class SanityTask extends BukkitRunnable {
    private final ObsidianSpireSanity plugin;
    private final SanityListener listener;

    public SanityTask(ObsidianSpireSanity plugin, SanityListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public void run() {
        if (plugin.getWipeEventManager().isWipeEventActive()) {
            return;
        }

        // 1. Process regular sanity changes
        plugin.getSanityManager().processSanityCycle();

        // 2. Process Hallucinations based on Sanity stage
        for (Player player : Bukkit.getOnlinePlayers()) {
            SanityManager.SanityStage stage = plugin.getSanityManager().getStage(player);
            ru.obsidianspire.sanity.data.PlayerData data = plugin.getPlayerData(player.getUniqueId());

            if (data != null && !data.isClozapineActive()) {
                triggerRandomHallucinationFor(player, stage);
            }

            // 3. Heavy Glance check (AFK > 30s)
            long lastMove = listener.getLastMoveTime(player.getUniqueId());
            if (System.currentTimeMillis() - lastMove > 30000 && stage.ordinal() >= SanityManager.SanityStage.PSYCHOSIS.ordinal()) {
                if (Math.random() < 0.3) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "heavy_glance");
                }
            }

            // 4. Staring into darkness 3+ minutes (-2% per 3 mins), and Light source healing (+3% per 5 mins in >= 10 light)
            int lightLevel = player.getLocation().getBlock().getLightLevel();
            if (lightLevel < 7) {
                Long darkStart = darkStareStart.get(player.getUniqueId());
                if (darkStart == null) {
                    darkStareStart.put(player.getUniqueId(), System.currentTimeMillis());
                } else if (System.currentTimeMillis() - darkStart >= 180000) { // 3 minutes
                    if (data != null && !data.isAminazineActive() && !data.isClozapineActive()) {
                        data.removeSanity(plugin.getConfigManager().sanityDropDarkStare);
                        darkStareStart.put(player.getUniqueId(), System.currentTimeMillis()); // reset
                    }
                }
            } else {
                darkStareStart.remove(player.getUniqueId());
            }

            if (lightLevel >= 10) {
                Long lightStart = lightHealStart.get(player.getUniqueId());
                if (lightStart == null) {
                    lightHealStart.put(player.getUniqueId(), System.currentTimeMillis());
                } else if (System.currentTimeMillis() - lightStart >= 300000) { // 5 minutes
                    if (data != null) {
                        data.addSanity(plugin.getConfigManager().sanityHealLightSource);
                        lightHealStart.put(player.getUniqueId(), System.currentTimeMillis()); // reset
                    }
                }
            } else {
                lightHealStart.remove(player.getUniqueId());
            }

            // 5. Tolerance Decay (1 level per 2 hours of not using)
            if (data != null) {
                long now = System.currentTimeMillis();
                long twoHours = 2 * 60 * 60 * 1000L;

                if (data.getToleranceAminazine() > 0 && now - data.getLastAminazineUse() > twoHours) {
                    data.setToleranceAminazine(data.getToleranceAminazine() - 1);
                    data.setLastAminazineUse(now); // reset timer to require another 2 hours for next drop
                }
                if (data.getToleranceHaloperidol() > 0 && now - data.getLastHaloperidolUse() > twoHours) {
                    data.setToleranceHaloperidol(data.getToleranceHaloperidol() - 1);
                    data.setLastHaloperidolUse(now);
                }
                if (data.getToleranceClozapine() > 0 && now - data.getLastClozapineUse() > twoHours) {
                    data.setToleranceClozapine(data.getToleranceClozapine() - 1);
                    data.setLastClozapineUse(now);
                }
            }
        }
    }

    private final java.util.Map<java.util.UUID, Long> darkStareStart = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Long> lightHealStart = new java.util.HashMap<>();

    public void removePlayerFromDarkStare(java.util.UUID uuid) {
        darkStareStart.remove(uuid);
    }

    public void removePlayerFromLightHeal(java.util.UUID uuid) {
        lightHealStart.remove(uuid);
    }

    private void triggerRandomHallucinationFor(Player player, SanityManager.SanityStage stage) {
        double rand = Math.random();

        switch (stage) {
            case ANXIETY: // 70-89
                if (rand < 0.1) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "fake_steps");
                } else if (rand < 0.15) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "echo_of_death"); // random sounds equivalent
                }
                break;
            case PARANOIA: // 50-69
                if (rand < 0.1) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "flickering_torches");
                } else if (rand < 0.2) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "eyes_in_the_crowd");
                } else if (rand < 0.3) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "phantom_hunger");
                } else if (rand < 0.4) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "fake_steps");
                }
                break;
            case PSYCHOSIS: // 30-49
                double baseP = 0.1; // phantom pursuit base
                double currP = baseP;

                if (rand < currP) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "phantom1_pursuit");
                } else if (rand < (currP += plugin.getConfigManager().chancePsychosisFalseDeath)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "false_death");
                } else if (rand < (currP += plugin.getConfigManager().chancePsychosisHeavyGlance)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "heavy_glance");
                } else if (rand < (currP += plugin.getConfigManager().chancePsychosisFlickeringTorches)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "flickering_torches");
                } else if (rand < (currP += plugin.getConfigManager().chancePsychosisEyesCrowd)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "eyes_in_the_crowd");
                } else if (rand < (currP += plugin.getConfigManager().chancePsychosisPhantomHunger)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "phantom_hunger");
                }
                break;
            case COLLAPSE: // 0-29
                double baseC = 0.15; // phantom pursuit base
                double currC = baseC;

                if (rand < currC) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "phantom1_pursuit");
                } else if (rand < (currC += plugin.getConfigManager().chanceCollapseFalseDeath)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "false_death");
                } else if (rand < (currC += plugin.getConfigManager().chanceCollapseHeavyGlance)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "heavy_glance");
                } else if (rand < (currC += plugin.getConfigManager().chanceCollapsePanicExplosion)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "panic_explosion");
                } else if (rand < (currC += plugin.getConfigManager().chanceCollapsePhantomFire)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "phantom_fire");
                } else if (rand < (currC += plugin.getConfigManager().chanceCollapseFalseInventory)) {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "false_inventory");
                } else {
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "chest_scream");
                }
                break;
            default:
                break;
        }
    }
}
