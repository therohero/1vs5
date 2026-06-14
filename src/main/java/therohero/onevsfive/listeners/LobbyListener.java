package therohero.onevsfive.listeners;

import therohero.onevsfive.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public class LobbyListener implements Listener {
    private final Plugin plugin;
    private final GameManager gameManager;

    public LobbyListener(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    private String getLobbyWorldName() {
        return plugin.getConfig().getString("lobby.world", "lobby");
    }

    private Location getLobbySpawn(World world) {
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig()
                .getConfigurationSection("lobby.spawn");
        if (section == null)
            return new Location(world, 0.5, 3.0, 0.5);
        return new Location(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0));
    }

    @EventHandler
    public void onPVP(EntityDamageByEntityEvent event) {
        if (event.getEntity().getWorld().getName().equals(getLobbyWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getWorld().getName().equals(getLobbyWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld().getName().equals(getLobbyWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Teleport to lobby
        World lobby = org.bukkit.Bukkit.getWorld(getLobbyWorldName());
        if (lobby == null) {
            lobby = org.bukkit.Bukkit.createWorld(new org.bukkit.WorldCreator(getLobbyWorldName()));
        }
        if (lobby != null) {
            player.teleport(getLobbySpawn(lobby));
        }

        if (gameManager.isCombatLogger(player.getUniqueId())) {
            player.getInventory().clear();
            player.sendMessage(Component.text("Du hast während eines Kampfes das Spiel verlassen (Combat-Log)!",
                    NamedTextColor.RED));
            gameManager.removeCombatLogger(player.getUniqueId());
        }

        if (gameManager.isGameRunning() || gameManager.isCountingDown()) {
            event.getPlayer().sendMessage(
                    Component.text("Es läuft bereits ein Match. Bitte warte in der Lobby!", NamedTextColor.GOLD));
            event.joinMessage(Component.text(event.getPlayer().getName(), NamedTextColor.YELLOW)
                    .append(Component.text(" ist beigetreten. Er wartet in der Lobby, da bereits ein Match läuft.",
                            NamedTextColor.GRAY)));
        } else {
            event.joinMessage(Component.text(event.getPlayer().getName(), NamedTextColor.GREEN)
                    .append(Component.text(" ist der Lobby beigetreten.", NamedTextColor.GRAY)));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!gameManager.isGameRunning()) {
            World lobby = org.bukkit.Bukkit.getWorld(getLobbyWorldName());
            if (lobby != null) {
                event.setRespawnLocation(getLobbySpawn(lobby));
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (event.getEntity().getWorld().getName().equals(getLobbyWorldName())) {
            event.setCancelled(true);
            event.getEntity().setFoodLevel(20);
        }
    }
}
