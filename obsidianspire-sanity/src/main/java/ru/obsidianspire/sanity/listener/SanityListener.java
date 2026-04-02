package ru.obsidianspire.sanity.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.data.PlayerData;
import ru.obsidianspire.sanity.manager.MedicineManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

public class SanityListener implements Listener {
    private final ObsidianSpireSanity plugin;
    private final Map<UUID, Long> lastMoveTime = new HashMap<>();

    public SanityListener(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager().hasMetadata("hallucination_phantom")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().hasMetadata("hallucination_phantom")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerData(player.getUniqueId()); // Load or create data
        lastMoveTime.put(player.getUniqueId(), System.currentTimeMillis());

        // Handle Wipe Event
        if (plugin.getWipeEventManager().isWipeEventActive()) {
            plugin.getWipeEventManager().handlePlayerJoin(player);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (plugin.getWipeEventManager().isWipeEventActive()) {
            org.bukkit.World whiteRoom = org.bukkit.Bukkit.getWorld(plugin.getConfigManager().whiteRoomWorldName);
            if (whiteRoom != null) {
                event.setRespawnLocation(whiteRoom.getSpawnLocation());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Ensure PhantomHunger restoration happens on quit if active
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data != null && data.getPhantomHungerOriginalFood() != -1) {
            player.setFoodLevel(data.getPhantomHungerOriginalFood());
            player.setSaturation(data.getPhantomHungerOriginalSat());
            data.setPhantomHungerOriginalFood(-1);
        }

        // Stop all hallucinations (this will remove Phantom1 zombie, HeavyGlance armorstand, etc.)
        plugin.getHallucinationManager().stopAllFor(player);

        // Save and unload data
        plugin.unloadPlayerData(player.getUniqueId());
        lastMoveTime.remove(player.getUniqueId());

        if (plugin.getSanityTask() != null) {
            plugin.getSanityTask().removePlayerFromDarkStare(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getX() == event.getTo().getX() &&
            event.getFrom().getY() == event.getTo().getY() &&
            event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }
        lastMoveTime.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerSleep(PlayerBedEnterEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());

        if (data != null) {
            if (data.getSanity() < 21.0) {
                event.setCancelled(true);
                player.sendMessage("§cВы не можете уснуть... Рассудок слишком низок.");
                return;
            }

            // Chance to trigger Phantom1 hallucination when trying to sleep
            ru.obsidianspire.sanity.manager.SanityManager.SanityStage stage = plugin.getSanityManager().getStage(player);
            if (stage.ordinal() >= ru.obsidianspire.sanity.manager.SanityManager.SanityStage.PSYCHOSIS.ordinal() && !data.isClozapineActive() && !data.isHaloperidolActive()) {
                double chance = stage == ru.obsidianspire.sanity.manager.SanityManager.SanityStage.COLLAPSE ? 0.4 : 0.2;
                if (Math.random() < chance) {
                    // Do NOT cancel the event, they need to get in bed to be woken up by the nightmare
                    plugin.getHallucinationManager().triggerSpecificHallucination(player, "phantom1_nightmare");
                }
            }
        }
    }

    @EventHandler
    public void onTimeSkip(org.bukkit.event.world.TimeSkipEvent event) {
        if (event.getSkipReason() == org.bukkit.event.world.TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            // Restore sanity for healthy sleep, decrease for sleepless
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                PlayerData data = plugin.getPlayerData(player.getUniqueId());
                if (data != null) {
                    if (player.isSleeping()) {
                        data.addSanity(plugin.getConfigManager().sanityHealSleep);
                    } else {
                        // Sleepless night
                        if (!data.isAminazineActive() && !data.isClozapineActive()) {
                            data.removeSanity(plugin.getConfigManager().sanityDropInsomnia);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            if (event.getEntity().getType() == org.bukkit.entity.EntityType.ENDERMAN) {
                PlayerData data = plugin.getPlayerData(player.getUniqueId());
                if (data != null && !data.isAminazineActive() && !data.isClozapineActive()) {
                    data.removeSanity(plugin.getConfigManager().sanityDropEnderman);
                    player.sendMessage("§5Чей-то взгляд проникает в ваш разум...");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        Player player = event.getPlayer();
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            // Check if it's a medicine (Mushroom stew is Tea, Potion is other)
            if (item.getType() == Material.POTION || item.getType() == Material.MUSHROOM_STEW) {
                MedicineManager.MedicineType type = getMedicineType(item);
                if (type != null) {
                    // Consume logic handled by PlayerItemConsumeEvent
                }
            }
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());

        // Phantom Hunger check
        if (data != null && data.getPhantomHungerOriginalFood() != -1) {
            // They ate during phantom hunger
            event.setCancelled(true);

            // Remove item they tried to eat
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                        player.getInventory().setItemInMainHand(null);
                    } else if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                        player.getInventory().setItemInOffHand(null);
                    }
                }
            }

            // Revert hunger to real value
            plugin.getHallucinationManager().stopSpecific(player, "phantom_hunger");
            return;
        }

        MedicineManager.MedicineType type = getMedicineType(item);
        if (type != null) {
            plugin.getMedicineManager().applyMedicine(player, type);
        }
    }

    // Custom Brewing Logic
    @EventHandler
    public void onBrew(BrewEvent event) {
        BrewerInventory inv = event.getContents();
        ItemStack ingredient = inv.getIngredient();
        if (ingredient == null) return;

        Material ingType = ingredient.getType();

        // Check for each bottle slot
        boolean changed = false;
        ItemStack[] results = new ItemStack[3];

        for (int i = 0; i < 3; i++) {
            ItemStack potion = inv.getItem(i);
            if (potion == null || potion.getType() == Material.AIR) continue;

            // Аминазин: Гнилая плоть + Мутное зелье (POTION)
            if (ingType == Material.ROTTEN_FLESH) {
                if (potion.getType() == Material.POTION && !potion.hasItemMeta()) { // Base mundane potion check simplified
                    results[i] = plugin.getMedicineManager().createMedicine(MedicineManager.MedicineType.AMINAZINE);
                    changed = true;
                }
            }
            // Галоперидол: Маринованный паучий глаз + Аминазин
            else if (ingType == Material.FERMENTED_SPIDER_EYE) {
                if (isMedicine(potion, MedicineManager.MedicineType.AMINAZINE)) {
                    results[i] = plugin.getMedicineManager().createMedicine(MedicineManager.MedicineType.HALOPERIDOL);
                    changed = true;
                }
            }
            // Клозапин: Слеза Гаста + Галоперидол
            else if (ingType == Material.GHAST_TEAR) {
                if (isMedicine(potion, MedicineManager.MedicineType.HALOPERIDOL)) {
                    results[i] = plugin.getMedicineManager().createMedicine(MedicineManager.MedicineType.CLOZAPINE);
                    changed = true;
                }
            }
        }

        if (changed) {
            event.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 3; i++) {
                        if (results[i] != null) {
                            inv.setItem(i, results[i]);
                        }
                    }
                    ingredient.setAmount(ingredient.getAmount() - 1);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private boolean isMedicine(ItemStack item, MedicineManager.MedicineType type) {
        if (item == null || !item.hasItemMeta()) return false;
        org.bukkit.persistence.PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        org.bukkit.NamespacedKey key = plugin.getMedicineManager().getMedicineKey();
        if (container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            String val = container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            return val != null && val.equals(type.name());
        }
        return false;
    }

    private MedicineManager.MedicineType getMedicineType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        org.bukkit.persistence.PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        org.bukkit.NamespacedKey key = plugin.getMedicineManager().getMedicineKey();
        if (container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            String val = container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
            if (val != null) {
                try {
                    return MedicineManager.MedicineType.valueOf(val);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        PlayerData data = plugin.getPlayerData(player.getUniqueId());

        if (data != null && data.isHallucinationActiveChestScream()) {
            if (event.getInventory().getType() == InventoryType.CHEST || event.getInventory().getType() == InventoryType.BARREL) {
                // Play scream
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);

                // Real items are hidden dynamically via ProtocolLib adapter registered in ObsidianSpireSanity
                // Automatically stop after 1 second (this will tell the packet adapter to stop intercepting)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            plugin.getHallucinationManager().stopSpecific(player, "chest_scream");
                            player.updateInventory(); // Force client to resync and show real items
                        }
                    }
                }.runTaskLater(plugin, 20L);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            PlayerData data = plugin.getPlayerData(player.getUniqueId());
            if (data != null && data.isHallucinationActiveChestScream()) {
                // Cancel any interactions while hallucination is active to prevent ghost item extraction
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        CraftingInventory inv = event.getInventory();
        boolean hasBowl = false;
        boolean hasDandelion = false;
        boolean hasDaisy = false;
        int count = 0;

        for (ItemStack item : inv.getMatrix()) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
                if (item.getType() == Material.BOWL) hasBowl = true;
                else if (item.getType() == Material.DANDELION) hasDandelion = true;
                else if (item.getType() == Material.OXEYE_DAISY) hasDaisy = true;
            }
        }

        if (count == 3 && hasBowl && hasDandelion && hasDaisy) {
            // It's herbal tea! We replace the recipe result
            inv.setResult(plugin.getMedicineManager().createMedicine(MedicineManager.MedicineType.HERBAL_TEA));
        }
    }

    public long getLastMoveTime(UUID uuid) {
        return lastMoveTime.getOrDefault(uuid, System.currentTimeMillis());
    }
}
