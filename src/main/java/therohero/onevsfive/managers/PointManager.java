package therohero.onevsfive.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PointManager {
    private final Map<UUID, Integer> points = new HashMap<>();
    private final File historyFile;
    private final LangManager lang;

    public PointManager(Plugin plugin, LangManager lang) {
        this.historyFile = new File(plugin.getDataFolder(), "history.yml");
        this.lang = lang;
    }

    public void addPoints(UUID uuid, int amount) {
        points.put(uuid, points.getOrDefault(uuid, 0) + amount);
    }

    public void resetPoints() {
        points.clear();
    }

    public Map<UUID, Integer> getPoints() {
        return Collections.unmodifiableMap(points);
    }

    public void recordHistory(String matchInfo) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(historyFile);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String key = "matches." + timestamp.replace(":", "-");

        config.set(key + ".info", matchInfo);

        List<Map.Entry<UUID, Integer>> sorted = points.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        List<String> results = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : sorted) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            results.add((name != null ? name : entry.getKey().toString()) + ": " + entry.getValue());
        }
        config.set(key + ".results", results);

        try {
            config.save(historyFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void displayLeaderboard() {
        if (points.isEmpty()) {
            Bukkit.broadcast(Component.text(lang.get("leaderboard_empty"), NamedTextColor.YELLOW));
            return;
        }

        List<Map.Entry<UUID, Integer>> sortedPoints = points.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        Component header = Component.text(lang.get("leaderboard_header"), NamedTextColor.GOLD, TextDecoration.BOLD);
        Bukkit.broadcast(header);

        for (int i = 0; i < sortedPoints.size(); i++) {
            Map.Entry<UUID, Integer> entry = sortedPoints.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null)
                name = "Unknown";

            String line = lang.get("leaderboard_entry", i + 1, name, entry.getValue());
            Bukkit.broadcast(Component.text(line, NamedTextColor.YELLOW));
        }
    }

    public void showHistory(org.bukkit.command.CommandSender sender) {
        if (!historyFile.exists()) {
            sender.sendMessage(Component.text(lang.get("history_none"), NamedTextColor.RED));
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(historyFile);
        if (!config.contains("matches")) {
            sender.sendMessage(Component.text(lang.get("history_no_matches"), NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text(lang.get("history_header"), NamedTextColor.GOLD, TextDecoration.BOLD));
        for (String key : config.getConfigurationSection("matches").getKeys(false)) {
            String timestamp = key.replace("-", ":");
            String info = config.getString("matches." + key + ".info");
            sender.sendMessage(Component.text("[" + timestamp + "] " + info, NamedTextColor.YELLOW));
            List<String> results = config.getStringList("matches." + key + ".results");
            for (String res : results) {
                sender.sendMessage(Component.text("  - " + res, NamedTextColor.GRAY));
            }
        }
    }
}