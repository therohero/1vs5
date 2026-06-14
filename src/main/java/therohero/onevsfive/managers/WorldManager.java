package therohero.onevsfive.managers;

import therohero.onevsfive.utils.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class WorldManager {
    private final Plugin plugin;
    private final String lobbyWorldName = "lobby";
    private final String fightWorldName = "fightworld";
    private final String backupWorldName = "fightworld_backup";

    public WorldManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void deleteFightWorld() {
        World fightWorld = Bukkit.getWorld(fightWorldName);
        if (fightWorld != null) {
            Bukkit.unloadWorld(fightWorld, false);
        }
        File fightDir = new File(Bukkit.getWorldContainer(), fightWorldName);
        if (fightDir.exists()) {
            try {
                FileUtils.deleteDirectory(fightDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public CompletableFuture<Boolean> resetArena() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // 1. Teleport all players to lobby
        World lobby = Bukkit.getWorld(lobbyWorldName);
        if (lobby == null) {
            lobby = new WorldCreator(lobbyWorldName).createWorld();
        }
        Location lobbySpawn = new Location(lobby, 0.5, 3, 0.5);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.teleport(lobbySpawn);
            player.clearActivePotionEffects();
        }

        // 2. Unload fightworld
        World fightWorld = Bukkit.getWorld(fightWorldName);
        if (fightWorld != null) {
            Bukkit.unloadWorld(fightWorld, false);
        }

        // 3. Delete and copy asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File fightDir = new File(Bukkit.getWorldContainer(), fightWorldName);
                File backupDir = new File(Bukkit.getWorldContainer(), backupWorldName);

                if (!backupDir.exists()) {
                    plugin.getLogger().severe("Backup world folder not found: " + backupWorldName);
                    future.complete(false);
                    return;
                }

                FileUtils.deleteDirectory(fightDir);
                FileUtils.copyDirectory(backupDir.getAbsolutePath(), fightDir.getAbsolutePath());

                // Delete uid.dat to avoid world ID conflicts
                File uidFile = new File(fightDir, "uid.dat");
                if (uidFile.exists())
                    uidFile.delete();

                // 4. Reload world on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.createWorld(new WorldCreator(fightWorldName));
                    future.complete(true);
                });

            } catch (IOException e) {
                e.printStackTrace();
                future.complete(false);
            }
        });

        return future;
    }
}
