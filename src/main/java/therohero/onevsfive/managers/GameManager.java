package therohero.onevsfive.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class GameManager {
    private final Plugin plugin;
    private final PointManager pointManager;
    private final WorldManager worldManager;
    private final KitManager kitManager;
    private final ScoreboardManager scoreboardManager;

    private UUID soloPlayer;
    private final Set<UUID> attackers = new HashSet<>();
    private final Set<UUID> aliveAttackers = new HashSet<>();
    private boolean gameRunning = false;
    private boolean isCountingDown = false;
    private int totalRounds = 3;
    private int currentRound = 0;
    private String selectedKit;
    private boolean dropItems = false;
    private final Set<UUID> combatLoggers = new HashSet<>();

    private final Map<Location, UUID> explosionSources = new HashMap<>();

    public GameManager(Plugin plugin, PointManager pointManager, WorldManager worldManager) {
        this.plugin = plugin;
        this.pointManager = pointManager;
        this.worldManager = worldManager;
        this.kitManager = new KitManager(plugin);
        this.scoreboardManager = new ScoreboardManager(this, pointManager);

        this.totalRounds = plugin.getConfig().getInt("game.default_rounds", 3);

        Bukkit.getScheduler().runTaskTimer(plugin, scoreboardManager::updateAll, 20L, 20L);
    }

    public void setSoloPlayer(UUID uuid) {
        this.soloPlayer = uuid;
        this.attackers.remove(uuid);
    }

    public void addAttacker(UUID uuid) {
        this.attackers.add(uuid);
        if (uuid.equals(soloPlayer))
            soloPlayer = null;
    }

    public void removeAttacker(UUID uuid) {
        this.attackers.remove(uuid);
    }

    public void setTotalRounds(int rounds) {
        this.totalRounds = rounds;
    }

    public void setSelectedKit(String kitName) {
        this.selectedKit = kitName;
    }

    public String getSelectedKit() {
        return selectedKit;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public void startMatch() {
        if (selectedKit == null) {
            Bukkit.broadcast(
                    Component.text("Match kann nicht gestartet werden: Kein Kit festgelegt!", NamedTextColor.RED));
            return;
        }
        if (!kitManager.kitExists(selectedKit)) {
            Bukkit.broadcast(
                    Component.text("Match kann nicht gestartet werden: Kit '" + selectedKit + "' existiert nicht mehr!",
                            NamedTextColor.RED));
            return;
        }
        if (soloPlayer == null || attackers.isEmpty()) {
            Bukkit.broadcast(Component.text("Match kann nicht gestartet werden: Solo oder Angreifer fehlen!",
                    NamedTextColor.RED));
            return;
        }
        currentRound = 0;
        pointManager.resetPoints();
        sendMatchIntroduction();
        startNextRound();
    }

    private void sendMatchIntroduction() {
        Component rules = Component.text("--- 1vs5 REGELN ---", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.newline())
                .append(Component.text("1. Der Solo-Spieler kämpft gegen das Team.", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("2. Friendly Fire zwischen Angreifern (Nahkampf & Explosionen) ist deaktiviert.",
                        NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text(
                        "3. Punkte: Solo-Kill (10 Pkt), Überleben als Angreifer (5 Pkt), Tod-Reihenfolge (1-5 Pkt).",
                        NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Viel Erfolg!", NamedTextColor.GREEN));
        Bukkit.broadcast(rules);
    }

    public void startNextRound() {
        if (currentRound >= totalRounds) {
            endMatch();
            return;
        }

        currentRound++;
        gameRunning = false;
        explosionSources.clear();

        Bukkit.broadcast(Component.text("Runde " + currentRound + " von " + totalRounds + " wird vorbereitet...",
                NamedTextColor.YELLOW));

        worldManager.resetArena().thenAccept(success -> {
            if (!success) {
                Bukkit.broadcast(Component.text("Fehler beim Zurücksetzen der Arena!", NamedTextColor.RED));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                preparePlayersForRound();
                startCountdown();
            });
        });
    }

    private void preparePlayersForRound() {
        aliveAttackers.clear();
        aliveAttackers.addAll(attackers);

        World fightWorld = Bukkit.getWorld(plugin.getConfig().getString("fightworld.world", "fightworld"));
        Location soloSpawn = getLocationFromConfig("fightworld.solo_spawn", fightWorld);
        Location attackersSpawn = getLocationFromConfig("fightworld.attackers_spawn", fightWorld);

        Player solo = Bukkit.getPlayer(soloPlayer);
        if (solo != null) {
            solo.setGameMode(GameMode.SURVIVAL);
            solo.teleport(soloSpawn);
            solo.setFireTicks(0);
            for (org.bukkit.potion.PotionEffect effect : solo.getActivePotionEffects()) {
                solo.removePotionEffect(effect.getType());
            }
            giveKit(solo);
        }

        for (UUID uuid : attackers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(attackersSpawn);
                p.setFireTicks(0);
                for (org.bukkit.potion.PotionEffect effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
                giveKit(p);
            }
        }
    }

    private void giveKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setSaturation(20);
        kitManager.loadKit(selectedKit, player);
    }

    private void startCountdown() {
        isCountingDown = true;
        int maxSeconds = plugin.getConfig().getInt("game.countdown_seconds", 5);
        new Runnable() {
            int seconds = maxSeconds;

            @Override
            public void run() {
                if (seconds > 0) {
                    Component msg = Component.text("Start in " + seconds + "...", NamedTextColor.GOLD,
                            TextDecoration.BOLD);
                    Bukkit.broadcast(msg);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.showTitle(Title.title(msg, Component.empty()));
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                    seconds--;
                    Bukkit.getScheduler().runTaskLater(plugin, this, 20L);
                } else {
                    isCountingDown = false;
                    gameRunning = true;
                    Bukkit.broadcast(Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD));
                }
            }
        }.run();
    }

    public void handleDeath(UUID victimUuid, UUID killerUuid) {
        if (!gameRunning)
            return;

        if (victimUuid.equals(soloPlayer)) {
            if (killerUuid != null && attackers.contains(killerUuid)) {
                pointManager.addPoints(killerUuid, 10);
                String killerName = Bukkit.getOfflinePlayer(killerUuid).getName();
                Bukkit.broadcast(Component.text((killerName != null ? killerName : "Ein Angreifer")
                        + " hat den Solo-Spieler eliminiert! (+10 Pkt)", NamedTextColor.GOLD));
            }

            for (UUID attackerId : aliveAttackers) {
                if (!attackerId.equals(killerUuid)) {
                    pointManager.addPoints(attackerId, 5);
                }
            }

            Bukkit.broadcast(Component.text("Der Solo-Spieler ist tot! Runde beendet.", NamedTextColor.RED));
            endRound();
        } else if (attackers.contains(victimUuid)) {
            if (aliveAttackers.remove(victimUuid)) {
                if (killerUuid != null && killerUuid.equals(soloPlayer)) {
                    pointManager.addPoints(soloPlayer, 1);
                    Player soloP = Bukkit.getPlayer(soloPlayer);
                    if (soloP != null)
                        soloP.sendMessage(Component.text("Gegner eliminiert! (+1 Punkt)", NamedTextColor.GOLD));
                }

                int points = (attackers.size() - aliveAttackers.size());
                pointManager.addPoints(victimUuid, points);
                Player p = Bukkit.getPlayer(victimUuid);
                if (p != null)
                    p.sendMessage(Component.text("Du erhältst " + points + " Punkt(e).", NamedTextColor.YELLOW));
            }

            if (aliveAttackers.isEmpty()) {
                Bukkit.broadcast(Component.text("Alle Angreifer sind tot! Der Solo-Spieler gewinnt die Runde! (+7 Pkt)",
                        NamedTextColor.GREEN));
                pointManager.addPoints(soloPlayer, 7);
                endRound();
            }
        }
    }

    public void handleQuit(Player player) {
        if (!gameRunning)
            return;
        UUID uuid = player.getUniqueId();

        if (uuid.equals(soloPlayer) || aliveAttackers.contains(uuid)) {
            combatLoggers.add(uuid);

            if (dropItems) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
            player.getInventory().clear();

            if (uuid.equals(soloPlayer)) {
                Bukkit.broadcast(
                        Component.text("Der Solo-Spieler hat das Spiel verlassen! Runde beendet.", NamedTextColor.RED));
                endRound();
            } else if (aliveAttackers.remove(uuid)) {
                if (aliveAttackers.isEmpty()) {
                    Bukkit.broadcast(Component.text("Alle Angreifer sind weg! Der Solo-Spieler gewinnt die Runde!",
                            NamedTextColor.GREEN));
                    pointManager.addPoints(soloPlayer, 7);
                    endRound();
                }
            }
        }
    }

    private void endRound() {
        gameRunning = false;
        long delay = plugin.getConfig().getInt("game.post_round_delay_seconds", 5) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, this::startNextRound, delay);
    }

    private void endMatch() {
        gameRunning = false;
        isCountingDown = false;
        combatLoggers.clear();

        Bukkit.broadcast(Component.text("Das Match ist beendet!", NamedTextColor.GOLD, TextDecoration.BOLD));
        pointManager.displayLeaderboard();
        pointManager.recordHistory("Match mit Kit: " + (selectedKit != null ? selectedKit : "Keins"));

        World lobby = Bukkit.getWorld(plugin.getConfig().getString("lobby.world", "lobby"));
        Location lobbySpawn = getLocationFromConfig("lobby.spawn", lobby);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(plugin.getConfig().getString("fightworld.world", "fightworld"))) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.getInventory().setItemInOffHand(null);
                p.teleport(lobbySpawn);
            }
        }
        worldManager.resetArena();
    }

    public void stopMatch() {
        gameRunning = false;
        isCountingDown = false;
        combatLoggers.clear();
        Bukkit.broadcast(Component.text("Match wurde abgebrochen.", NamedTextColor.RED));

        World lobby = Bukkit.getWorld(plugin.getConfig().getString("lobby.world", "lobby"));
        Location lobbySpawn = getLocationFromConfig("lobby.spawn", lobby);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(plugin.getConfig().getString("fightworld.world", "fightworld"))) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.getInventory().setItemInOffHand(null);
                p.teleport(lobbySpawn);
            }
        }
    }

    private Location getLocationFromConfig(String path, World world) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null)
            return world.getSpawnLocation();
        return new Location(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw", 0),
                (float) section.getDouble("pitch", 0));
    }

    public void spectateFight(Player player) {
        World fightWorld = Bukkit.getWorld(plugin.getConfig().getString("fightworld.world", "fightworld"));
        if (fightWorld == null) {
            player.sendMessage(Component.text("Die Fightworld ist aktuell nicht geladen!", NamedTextColor.RED));
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);

        Player solo = Bukkit.getPlayer(soloPlayer);
        if (solo != null && solo.getWorld().equals(fightWorld)) {
            player.teleport(solo.getLocation());
        } else {
            player.teleport(fightWorld.getSpawnLocation());
        }

        player.sendMessage(
                Component.text("Du schaust dem Kampf nun zu (Teleport zum Solo-Spieler).", NamedTextColor.GRAY));
    }

    public boolean isCombatLogger(UUID uuid) {
        return combatLoggers.contains(uuid);
    }

    public void removeCombatLogger(UUID uuid) {
        combatLoggers.remove(uuid);
    }

    public void trackExplosionSource(Location loc, UUID owner) {
        explosionSources.put(loc.getBlock().getLocation(), owner);
        Bukkit.getScheduler().runTaskLater(plugin, () -> explosionSources.remove(loc.getBlock().getLocation()), 10000L);
    }

    public UUID getExplosionSource(Location loc) {
        for (Map.Entry<Location, UUID> entry : explosionSources.entrySet()) {
            if (entry.getKey().getWorld().equals(loc.getWorld()) && entry.getKey().distanceSquared(loc) < 100) {
                return entry.getValue();
            }
        }
        return null;
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public boolean isCountingDown() {
        return isCountingDown;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getAliveAttackersCount() {
        return aliveAttackers.size();
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    public Set<UUID> getAttackers() {
        return attackers;
    }

    public UUID getSoloPlayer() {
        return soloPlayer;
    }
}
