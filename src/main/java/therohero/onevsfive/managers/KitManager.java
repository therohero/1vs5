package therohero.onevsfive.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class KitManager {
    private final File kitFolder;

    public KitManager(Plugin plugin) {
        this.kitFolder = new File(plugin.getDataFolder(), "kits");
        if (!kitFolder.exists()) {
            kitFolder.mkdirs();
        }
    }

    public void saveKit(String name, Player player, boolean naturalRegen) throws IOException {
        File file = new File(kitFolder, name + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("inventory", player.getInventory().getStorageContents());
        config.set("armor", player.getInventory().getArmorContents());
        config.set("offhand", player.getInventory().getExtraContents());

        if (!naturalRegen) {
            config.set("natural_regeneration", true);
        }
        // If true (default), key is simply not written

        config.save(file);
    }

    public boolean deleteKit(String name) {
        File file = new File(kitFolder, name + ".yml");
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public boolean kitExists(String name) {
        return new File(kitFolder, name + ".yml").exists();
    }

    public void loadKit(String name, Player player) {
        loadKit(name, player, true);
    }

    public void loadKit(String name, Player player, boolean updateGameRule) {
        File file = new File(kitFolder, name + ".yml");
        if (!file.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        @SuppressWarnings("unchecked")
        ItemStack[] inventory = ((List<ItemStack>) config.get("inventory")).toArray(new ItemStack[0]);
        @SuppressWarnings("unchecked")
        ItemStack[] armor = ((List<ItemStack>) config.get("armor")).toArray(new ItemStack[0]);
        @SuppressWarnings("unchecked")
        ItemStack[] offhand = ((List<ItemStack>) config.get("offhand")).toArray(new ItemStack[0]);

        player.getInventory().setStorageContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setExtraContents(offhand);

        if (updateGameRule) {
            // Set naturalRegeneration gamerule on der fightworld basierend auf
            // Kit-Einstellung
            // Standard: AN — nur geschrieben wenn AUS
            boolean naturalRegen = config.getBoolean("natural_regeneration", true);
            org.bukkit.World world = player.getWorld();
            String value = naturalRegen ? "true" : "false";
            // Use string API to bypass Java enum deprecation issues (works on all Paper
            // 1.21 builds)
            if (!world.setGameRuleValue("minecraft:natural_health_regeneration", value)) {
                // Fallback for older builds that still use the old name
                world.setGameRuleValue("naturalRegeneration", value);
            }
        }
    }

    public List<String> getKitNames() {
        String[] files = kitFolder.list();
        if (files == null)
            return java.util.Collections.emptyList();
        java.util.List<String> list = new java.util.ArrayList<>();
        for (String f : files) {
            if (f.endsWith(".yml"))
                list.add(f.substring(0, f.length() - 4));
        }
        return list;
    }
}
