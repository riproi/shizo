package ru.obsidianspire.sanity.hallucination;

import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.manager.SanityManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HallucinationManager {
    private final ObsidianSpireSanity plugin;
    private final Map<UUID, List<Hallucination>> activeHallucinations = new ConcurrentHashMap<>();

    public HallucinationManager(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
    }

    public void triggerRandomHallucination(Player player, SanityManager.SanityStage stage) {
        // Based on stage, probability of different hallucinations
        // Implement logic to randomly pick and trigger a continuous/one-time hallucination
    }

    public void triggerSpecificHallucination(Player player, String type) {
        Hallucination hallucination = null;
        switch (type.toLowerCase()) {
            case "phantom1":
            case "phantom1_pursuit":
                hallucination = new Phantom1PursuitHallucination(plugin, player);
                break;
            case "phantom1_nightmare":
                hallucination = new Phantom1NightmareHallucination(plugin, player);
                break;
            case "false_death":
                hallucination = new FalseDeathHallucination(plugin, player);
                break;
            case "heavy_glance":
                hallucination = new HeavyGlanceHallucination(plugin, player);
                break;
            case "chest_scream":
                hallucination = new ChestScreamHallucination(plugin, player);
                break;
            case "fake_steps":
                hallucination = new FakeStepsHallucination(plugin, player);
                break;
            case "phantom_fire":
                hallucination = new PhantomFireHallucination(plugin, player);
                break;
            case "panic_explosion":
                hallucination = new PanicExplosionHallucination(plugin, player);
                break;
            case "echo_of_death":
                hallucination = new EchoOfDeathHallucination(plugin, player);
                break;
            case "false_inventory":
                hallucination = new FalseInventoryHallucination(plugin, player);
                break;
            case "phantom_hunger":
                hallucination = new PhantomHungerHallucination(plugin, player);
                break;
            case "flickering_torches":
                hallucination = new FlickeringTorchesHallucination(plugin, player);
                break;
            case "eyes_in_the_crowd":
                hallucination = new EyesInTheCrowdHallucination(plugin, player);
                break;
        }

        if (hallucination != null) {
            hallucination.trigger();
            activeHallucinations.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(hallucination);
            if (plugin.getConfigManager().debug) {
                plugin.getLogger().info("[Debug] Triggered hallucination '" + type + "' for player " + player.getName());
            }
        }
    }

    public void stopAllFor(Player player) {
        List<Hallucination> list = activeHallucinations.remove(player.getUniqueId());
        if (list != null) {
            for (Hallucination h : list) {
                h.stop();
            }
        }
    }

    public void stopSpecific(Player player, String type) {
        List<Hallucination> list = activeHallucinations.get(player.getUniqueId());
        if (list != null) {
            Iterator<Hallucination> iterator = list.iterator();
            while (iterator.hasNext()) {
                Hallucination h = iterator.next();
                if (h.getClass().getSimpleName().toLowerCase().startsWith(type.replace("_", ""))) {
                    h.stop();
                    iterator.remove();
                }
            }
        }
    }
}
