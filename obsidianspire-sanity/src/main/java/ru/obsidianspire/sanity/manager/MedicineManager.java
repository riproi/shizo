package ru.obsidianspire.sanity.manager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import ru.obsidianspire.sanity.ObsidianSpireSanity;
import ru.obsidianspire.sanity.data.PlayerData;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import java.util.List;

public class MedicineManager {
    private final ObsidianSpireSanity plugin;
    private final NamespacedKey medicineKey;

    public MedicineManager(ObsidianSpireSanity plugin) {
        this.plugin = plugin;
        this.medicineKey = new NamespacedKey(plugin, "medicine_type");
    }

    public NamespacedKey getMedicineKey() {
        return medicineKey;
    }

    public enum MedicineType {
        AMINAZINE("Аминазин", ChatColor.YELLOW),
        HALOPERIDOL("Галоперидол", ChatColor.RED),
        CLOZAPINE("Клозапин", ChatColor.DARK_PURPLE),
        HERBAL_TEA("Травяной чай", ChatColor.GREEN);

        private final String name;
        private final ChatColor color;

        MedicineType(String name, ChatColor color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return color + name;
        }
    }

    public ItemStack createMedicine(MedicineType type) {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(type.getName());
        List<String> lore = new ArrayList<>();

        // Set NBT data to prevent anvil renaming exploit
        meta.getPersistentDataContainer().set(medicineKey, PersistentDataType.STRING, type.name());

        switch (type) {
            case AMINAZINE:
                meta.setBasePotionType(PotionType.MUNDANE);
                meta.setColor(org.bukkit.Color.YELLOW);
                lore.add(ChatColor.GRAY + "Отключает падение рассудка на 15 мин.");
                break;
            case HALOPERIDOL:
                meta.setBasePotionType(PotionType.MUNDANE);
                meta.setColor(org.bukkit.Color.RED);
                lore.add(ChatColor.GRAY + "Блокирует Фантома1, Ложную смерть,");
                lore.add(ChatColor.GRAY + "Тяжёлый взгляд на 10 мин.");
                break;
            case CLOZAPINE:
                meta.setBasePotionType(PotionType.MUNDANE);
                meta.setColor(org.bukkit.Color.PURPLE);
                lore.add(ChatColor.GRAY + "Очищает все галлюцинации на 20 мин.");
                lore.add(ChatColor.GRAY + "+20% рассудка.");
                break;
            case HERBAL_TEA:
                item.setType(Material.MUSHROOM_STEW);
                meta = (PotionMeta) Bukkit.getItemFactory().getItemMeta(Material.POTION); // Just for color if needed, but mushroom stew doesn't use it.
                lore.add(ChatColor.GRAY + "+5% рассудка.");
                break;
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void applyMedicine(Player player, MedicineType type) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        if (data == null) return;

        long currentTime = System.currentTimeMillis();

        switch (type) {
            case AMINAZINE:
                data.addToleranceAminazine();
                int tolA = data.getToleranceAminazine();
                double multiA = tolA > 2 ? 0.5 : 1.0;
                long durA = (long) (15 * 60 * 1000 * multiA);
                data.setAminazineActiveUntil(currentTime + durA);

                int lvlA = tolA > 2 ? Math.min(tolA - 2, 4) : 0;
                int poDurA = tolA > 2 ? 1 : 0;

                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (3 + poDurA * 1) * 60 * 20, lvlA));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (1 + poDurA * 0) * 60 * 20, lvlA)); // Nausea
                break;

            case HALOPERIDOL:
                data.addToleranceHaloperidol();
                int tolH = data.getToleranceHaloperidol();
                double multiH = tolH > 2 ? 0.5 : 1.0;
                long durH = (long) (10 * 60 * 1000 * multiH);
                data.setHaloperidolActiveUntil(currentTime + durH);

                int lvlH = tolH > 2 ? Math.min(tolH - 2 + 1, 4) : 1; // Base is II (index 1)
                int poDurH = tolH > 2 ? 2 : 0;

                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (5 + poDurH) * 60 * 20, lvlH));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (3 + poDurH) * 60 * 20, lvlH));
                break;

            case CLOZAPINE:
                data.addToleranceClozapine();
                int tolC = data.getToleranceClozapine();
                double multiC = tolC > 2 ? 0.5 : 1.0;
                long durC = (long) (20 * 60 * 1000 * multiC);
                data.setClozapineActiveUntil(currentTime + durC);
                data.addSanity(20.0);

                int lvlC = tolC > 2 ? Math.min(tolC - 2 + 2, 4) : 2; // Base is III (index 2)
                int poDurC = tolC > 2 ? 2 : 0;

                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, (5 + poDurC) * 60 * 20, lvlC)); // Fatigue
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, (4 + poDurC) * 60 * 20, lvlC));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, (5 + poDurC) * 60 * 20, lvlC));
                break;

            case HERBAL_TEA:
                data.addSanity(5.0);
                break;
        }
    }
}
