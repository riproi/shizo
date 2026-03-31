package ru.obsidianspire.sanity;

import org.bukkit.plugin.java.JavaPlugin;
import ru.obsidianspire.sanity.data.DataProvider;
import ru.obsidianspire.sanity.data.SQLiteProvider;
import ru.obsidianspire.sanity.data.PlayerData;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class ObsidianSpireSanity extends JavaPlugin {

    private static ObsidianSpireSanity instance;
    private DataProvider dataProvider;
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    private ru.obsidianspire.sanity.manager.SanityManager sanityManager;
    private ru.obsidianspire.sanity.manager.MedicineManager medicineManager;
    private ru.obsidianspire.sanity.manager.WipeEventManager wipeEventManager;
    private ru.obsidianspire.sanity.hallucination.HallucinationManager hallucinationManager;
    private ru.obsidianspire.sanity.listener.SanityListener sanityListener;
    private ru.obsidianspire.sanity.listener.SanityTask sanityTask;
    private ru.obsidianspire.sanity.config.ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // Configuration
        this.configManager = new ru.obsidianspire.sanity.config.ConfigManager(this);

        // Initialize Database
        this.dataProvider = new SQLiteProvider(this);
        this.dataProvider.init();

        // Initialize Managers
        this.sanityManager = new ru.obsidianspire.sanity.manager.SanityManager(this);
        this.medicineManager = new ru.obsidianspire.sanity.manager.MedicineManager(this);
        this.wipeEventManager = new ru.obsidianspire.sanity.manager.WipeEventManager(this);
        this.hallucinationManager = new ru.obsidianspire.sanity.hallucination.HallucinationManager(this);

        // Register Listeners
        this.sanityListener = new ru.obsidianspire.sanity.listener.SanityListener(this);
        getServer().getPluginManager().registerEvents(sanityListener, this);

        // Start Tasks
        this.sanityTask = new ru.obsidianspire.sanity.listener.SanityTask(this, sanityListener);
        this.sanityTask.runTaskTimer(this, 1200L, 1200L); // Every minute (Synchronously to allow Bukkit API calls safely)

        // ProtocolLib Handlers
        registerProtocolLibAdapters();

        // Register Commands
        getCommand("sanity").setExecutor(new ru.obsidianspire.sanity.command.SanityCommand(this));
        getCommand("gh").setExecutor(new ru.obsidianspire.sanity.command.HallucinationCommand(this));
        getCommand("meds").setExecutor(new ru.obsidianspire.sanity.command.MedicineCommand(this));

        // Register Recipe for Herbal Tea
        NamespacedKey teaKey = new NamespacedKey(this, "herbal_tea");
        ShapelessRecipe teaRecipe = new ShapelessRecipe(teaKey, this.medicineManager.createMedicine(ru.obsidianspire.sanity.manager.MedicineManager.MedicineType.HERBAL_TEA));
        teaRecipe.addIngredient(Material.BOWL);
        teaRecipe.addIngredient(Material.DANDELION);
        teaRecipe.addIngredient(Material.OXEYE_DAISY);
        getServer().addRecipe(teaRecipe);

        getLogger().info("ObsidianSpire Sanity Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all data
        for (PlayerData data : playerDataCache.values()) {
            dataProvider.savePlayerData(data);
        }

        if (this.dataProvider != null) {
            this.dataProvider.close();
        }

        getLogger().info("ObsidianSpire Sanity Plugin has been disabled!");
    }

    public static ObsidianSpireSanity getInstance() {
        return instance;
    }

    public DataProvider getDataProvider() {
        return dataProvider;
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, k -> dataProvider.loadPlayerData(k));
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.remove(uuid);
        if (data != null) {
            dataProvider.savePlayerData(data);
        }
    }

    public Map<UUID, PlayerData> getPlayerDataCache() {
        return playerDataCache;
    }

    public ru.obsidianspire.sanity.config.ConfigManager getConfigManager() {
        return configManager;
    }

    public ru.obsidianspire.sanity.manager.SanityManager getSanityManager() {
        return sanityManager;
    }

    public ru.obsidianspire.sanity.manager.MedicineManager getMedicineManager() {
        return medicineManager;
    }

    public ru.obsidianspire.sanity.manager.WipeEventManager getWipeEventManager() {
        return wipeEventManager;
    }

    public ru.obsidianspire.sanity.hallucination.HallucinationManager getHallucinationManager() {
        return hallucinationManager;
    }

    public ru.obsidianspire.sanity.listener.SanityTask getSanityTask() {
        return sanityTask;
    }

    private void registerProtocolLibAdapters() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.WINDOW_ITEMS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlayerData data = getPlayerData(player.getUniqueId());

                if (data != null && data.isHallucinationActiveChestScream()) {
                    List<ItemStack> items = event.getPacket().getItemListModifier().read(0);
                    if (items != null) {
                        List<ItemStack> fakeItems = new java.util.ArrayList<>();
                        for (ItemStack item : items) {
                            if (item != null && item.getType() != Material.AIR) {
                                fakeItems.add(new ItemStack(Material.ROTTEN_FLESH, item.getAmount()));
                            } else {
                                fakeItems.add(new ItemStack(Material.AIR));
                            }
                        }
                        event.getPacket().getItemListModifier().write(0, fakeItems);
                    }
                }
            }
        });

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.SET_SLOT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                PlayerData data = getPlayerData(player.getUniqueId());

                if (data != null && data.isHallucinationActiveChestScream()) {
                    ItemStack item = event.getPacket().getItemModifier().read(0);
                    if (item != null && item.getType() != Material.AIR) {
                        event.getPacket().getItemModifier().write(0, new ItemStack(Material.ROTTEN_FLESH, item.getAmount()));
                    }
                }
            }
        });
    }
}
