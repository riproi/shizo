package ru.obsidianspire.sanity.hallucination;

import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;

public abstract class Hallucination {
    protected final ObsidianSpireSanity plugin;
    protected final Player player;

    public Hallucination(ObsidianSpireSanity plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public abstract void trigger();
    public abstract void stop();
}
