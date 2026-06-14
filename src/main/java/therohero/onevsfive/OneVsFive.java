package therohero.onevsfive;

import therohero.onevsfive.commands.AdminCommand;
import therohero.onevsfive.commands.SpectateCommand;
import therohero.onevsfive.listeners.GameListener;
import therohero.onevsfive.listeners.LobbyListener;
import therohero.onevsfive.managers.GameManager;
import therohero.onevsfive.managers.PointManager;
import therohero.onevsfive.managers.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class OneVsFive extends JavaPlugin {

    private GameManager gameManager;
    private PointManager pointManager;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize Managers
        this.pointManager = new PointManager(this);
        this.worldManager = new WorldManager(this);
        this.gameManager = new GameManager(this, pointManager, worldManager);

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(new LobbyListener(this, gameManager), this);
        Bukkit.getPluginManager().registerEvents(new GameListener(this, gameManager), this);

        // Register Commands
        AdminCommand adminCommand = new AdminCommand(this, gameManager, pointManager);
        getCommand("1vs5").setExecutor(adminCommand);
        getCommand("1vs5").setTabCompleter(adminCommand);
        getCommand("spectatefight").setExecutor(new SpectateCommand(gameManager));

        getLogger().info("1vs5 Advanced Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (worldManager != null) {
            worldManager.deleteFightWorld();
        }
        getLogger().info("1vs5 Plugin disabled!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public PointManager getPointManager() {
        return pointManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}