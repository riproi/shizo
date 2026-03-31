package ru.obsidianspire.sanity.hallucination;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import org.bukkit.Location;

public class EchoOfDeathHallucination extends Hallucination {

    public EchoOfDeathHallucination(ObsidianSpireSanity plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public void trigger() {
        if (!player.isOnline()) return;

        Location soundLoc = player.getLocation().add(player.getLocation().getDirection().multiply(-2)); // Behind
        player.playSound(soundLoc, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);

        // Loss of sanity upon turning around is handled in SanityListener
    }

    @Override
    public void stop() {
        // Instant
    }
}
