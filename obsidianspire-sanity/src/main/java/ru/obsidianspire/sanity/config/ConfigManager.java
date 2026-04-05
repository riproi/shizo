package ru.obsidianspire.sanity.config;

import org.bukkit.configuration.file.FileConfiguration;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public class ConfigManager {
    private final ObsidianSpireSanity plugin;

    public boolean debugMode;
    public String whiteRoomWorldName;
    public boolean wipeEventActive;

    // Sanity drop rates
    public double sanityDropDarkness;
    public double sanityDropIsolation;
    public double sanityDropInsomnia;
    public double sanityDropEnderman;
    public double sanityDropPhantom1Contact;
    public double sanityDropDarkStare;

    // Healing rates
    public double sanityHealSleep;
    public double sanityHealGroupTherapy;
    public double sanityHealLightSource;

    // Text & Messages
    public String textPhantom1Pursuit;
    public String textPhantom1Nightmare;

    // Hallucination chances (Psychosis & Collapse)
    public double chancePsychosisFalseDeath;
    public double chancePsychosisHeavyGlance;
    public double chancePsychosisFlickeringTorches;
    public double chancePsychosisEyesCrowd;
    public double chancePsychosisPhantomHunger;

    public double chanceCollapseFalseDeath;
    public double chanceCollapseHeavyGlance;
    public double chanceCollapsePanicExplosion;
    public double chanceCollapsePhantomFire;
    public double chanceCollapseFalseInventory;

    public boolean debug;

    public ConfigManager(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadConfig();
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        debugMode = config.getBoolean("debug", false);
        whiteRoomWorldName = config.getString("wipe_event.world_name", "sanity_white_room");
        wipeEventActive = config.getBoolean("wipe_event.wipe_event_active", false);

        sanityDropDarkness = config.getDouble("sanity.drops.darkness_per_min", 1.0);
        sanityDropIsolation = config.getDouble("sanity.drops.isolation_per_10min", 5.0);
        sanityDropInsomnia = config.getDouble("sanity.drops.insomnia_per_night", 5.0);
        sanityDropEnderman = config.getDouble("sanity.drops.enderman_look", 10.0);
        sanityDropPhantom1Contact = config.getDouble("sanity.drops.phantom1_contact", 10.0);
        sanityDropDarkStare = config.getDouble("sanity.drops.staring_into_darkness_3min", 2.0);

        sanityHealSleep = config.getDouble("sanity.heals.healthy_sleep", 15.0);
        sanityHealGroupTherapy = config.getDouble("sanity.heals.group_therapy_per_min", 5.0);
        sanityHealLightSource = config.getDouble("sanity.heals.light_source_5min", 3.0);

        textPhantom1Pursuit = config.getString("texts.phantom1_pursuit", "§cЭто всего лишь сон...");
        textPhantom1Nightmare = config.getString("texts.phantom1_nightmare", "§cТолько не снова...");

        chancePsychosisFalseDeath = config.getDouble("chances.psychosis.false_death", 0.1);
        chancePsychosisHeavyGlance = config.getDouble("chances.psychosis.heavy_glance", 0.1);
        chancePsychosisFlickeringTorches = config.getDouble("chances.psychosis.flickering_torches", 0.1);
        chancePsychosisEyesCrowd = config.getDouble("chances.psychosis.eyes_in_the_crowd", 0.1);
        chancePsychosisPhantomHunger = config.getDouble("chances.psychosis.phantom_hunger", 0.1);

        chanceCollapseFalseDeath = config.getDouble("chances.collapse.false_death", 0.15);
        chanceCollapseHeavyGlance = config.getDouble("chances.collapse.heavy_glance", 0.15);
        chanceCollapsePanicExplosion = config.getDouble("chances.collapse.panic_explosion", 0.15);
        chanceCollapsePhantomFire = config.getDouble("chances.collapse.phantom_fire", 0.15);
        chanceCollapseFalseInventory = config.getDouble("chances.collapse.false_inventory", 0.15);

        debug = config.getBoolean("debug", false);
    }
}
