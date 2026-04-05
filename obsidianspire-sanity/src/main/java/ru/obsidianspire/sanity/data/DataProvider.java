package ru.obsidianspire.sanity.data;

import java.util.UUID;

public interface DataProvider {
    void init();
    PlayerData loadPlayerData(UUID uuid);
    void savePlayerData(PlayerData data);
    void close();
}
