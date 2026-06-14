package therohero.onevsfive.listeners;

import therohero.onevsfive.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class GameListener implements Listener {
    private final Plugin plugin;
    private final GameManager gameManager;

    public GameListener(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (gameManager.isCountingDown()) {
            if (event.hasChangedBlock()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!gameManager.isDropItems()) {
            event.getDrops().clear();
        }
        event.setKeepInventory(false);
        event.setKeepLevel(false);

        if (!gameManager.isGameRunning())
            return;

        Player victim = event.getEntity();
        UUID victimUuid = victim.getUniqueId();

        UUID killerUuid = null;
        Player killer = victim.getKiller();
        if (killer != null) {
            killerUuid = killer.getUniqueId();
        } else if (victim.hasMetadata("last_explosion_killer")) {
            String value = victim.getMetadata("last_explosion_killer").get(0).asString();
            killerUuid = UUID.fromString(value);
            victim.removeMetadata("last_explosion_killer", plugin);
        }

        gameManager.handleDeath(victimUuid, killerUuid);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            victim.spigot().respawn();
            victim.setGameMode(GameMode.SPECTATOR);

            Player solo = Bukkit.getPlayer(gameManager.getSoloPlayer());
            if (solo != null && solo.getWorld().getName().equals("fightworld")) {
                victim.teleport(solo.getLocation());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        gameManager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!gameManager.isGameRunning())
            return;
        if (!(event.getEntity() instanceof Player victim))
            return;

        UUID damagerUuid = null;

        if (event.getDamager() instanceof Player attacker) {
            damagerUuid = attacker.getUniqueId();
        } else if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            damagerUuid = shooter.getUniqueId();
        } else if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            damagerUuid = gameManager.getExplosionSource(event.getDamager().getLocation());
        }

        if (damagerUuid == null)
            return;

        if (gameManager.getAttackers().contains(victim.getUniqueId()) &&
                gameManager.getAttackers().contains(damagerUuid)) {

            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK ||
                    event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                event.setCancelled(true);
                return;
            }
        }

        if (victim.getUniqueId().equals(gameManager.getSoloPlayer())) {
            victim.setMetadata("last_explosion_killer", new FixedMetadataValue(plugin, damagerUuid.toString()));
        }
    }

    @EventHandler
    public void onExplosionDamage(EntityDamageEvent event) {
        if (!gameManager.isGameRunning())
            return;
        if (!(event.getEntity() instanceof Player victim))
            return;
        if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION &&
                event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
            return;

        UUID sourceUuid = gameManager.getExplosionSource(victim.getLocation());

        if (sourceUuid != null) {
            victim.setMetadata("last_explosion_killer", new FixedMetadataValue(plugin, sourceUuid.toString()));
        }
    }

    @EventHandler
    public void onAnchorCharge(PlayerInteractEvent event) {
        if (!gameManager.isGameRunning())
            return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR)
            return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;

        gameManager.trackExplosionSource(event.getClickedBlock().getLocation(), event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCrystalHit(EntityDamageByEntityEvent event) {
        if (!gameManager.isGameRunning())
            return;
        if (event.getEntity() instanceof EnderCrystal && event.getDamager() instanceof Player damager) {
            gameManager.trackExplosionSource(event.getEntity().getLocation(), damager.getUniqueId());
        }
    }
}