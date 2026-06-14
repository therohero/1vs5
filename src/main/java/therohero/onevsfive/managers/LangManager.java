package therohero.onevsfive.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LangManager {

    private final Plugin plugin;
    private YamlConfiguration lang;

    public LangManager(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String language = plugin.getConfig().getString("language", "en");
        String fileName = language + ".yml";

        // Save default language file if it doesn't exist yet
        File langFile = new File(plugin.getDataFolder(), "languages" + File.separator + fileName);
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            plugin.saveResource("languages/" + fileName, false);
        }

        lang = YamlConfiguration.loadConfiguration(langFile);

        // Fill in any missing keys from the bundled default
        InputStream defaultStream = plugin.getResource("languages/" + fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            lang.setDefaults(defaults);
        }
    }

    /**
     * Returns the raw translation string for the given key.
     */
    public String get(String key) {
        return lang.getString(key, key);
    }

    /**
     * Returns the translation string formatted with the given arguments
     * (String.format style).
     */
    public String get(String key, Object... args) {
        String raw = lang.getString(key, key);
        try {
            return String.format(raw, args);
        } catch (Exception e) {
            return raw;
        }
    }
}