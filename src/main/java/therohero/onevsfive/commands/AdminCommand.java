package therohero.onevsfive.commands;

import therohero.onevsfive.managers.GameManager;
import therohero.onevsfive.managers.PointManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final GameManager gameManager;
    private final PointManager pointManager;
    private final org.bukkit.plugin.Plugin plugin;

    public AdminCommand(org.bukkit.plugin.Plugin plugin, GameManager gameManager, PointManager pointManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.pointManager = pointManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("Keine Berechtigung!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setsolo":
                if (args.length < 2)
                    return false;
                Player solo = Bukkit.getPlayer(args[1]);
                if (solo != null) {
                    gameManager.setSoloPlayer(solo.getUniqueId());
                    sender.sendMessage(Component.text(solo.getName() + " ist Solo.", NamedTextColor.GREEN));
                }
                break;
            case "addattacker":
                if (args.length < 2)
                    return false;
                Player att = Bukkit.getPlayer(args[1]);
                if (att != null) {
                    gameManager.addAttacker(att.getUniqueId());
                    sender.sendMessage(Component.text(att.getName() + " ist Angreifer.", NamedTextColor.GREEN));
                }
                break;
            case "removeattacker":
                if (args.length < 2)
                    return false;
                Player rem = Bukkit.getPlayer(args[1]);
                if (rem != null) {
                    gameManager.removeAttacker(rem.getUniqueId());
                    sender.sendMessage(Component.text(rem.getName() + " entfernt.", NamedTextColor.YELLOW));
                }
                break;
            case "rounds":
                if (args.length < 2)
                    return false;
                try {
                    int r = Integer.parseInt(args[1]);
                    gameManager.setTotalRounds(r);
                    sender.sendMessage(Component.text("Runden auf " + r + " gesetzt.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Ungültige Zahl!", NamedTextColor.RED));
                }
                break;
            case "savekit":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Nur Spieler können Kits speichern!", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2)
                    return false;
                try {
                    boolean naturalRegen = args.length >= 3 && args[2].equalsIgnoreCase("regen");
                    gameManager.getKitManager().saveKit(args[1], player, naturalRegen);
                    sender.sendMessage(Component.text(
                            "Kit '" + args[1] + "' gespeichert" + (naturalRegen ? " (natürliche Regeneration: AN)" : "")
                                    + ".",
                            NamedTextColor.GREEN));
                } catch (Exception e) {
                    sender.sendMessage(Component.text("Fehler beim Speichern des Kits!", NamedTextColor.RED));
                    e.printStackTrace();
                }
                break;
            case "setkit":
                if (args.length < 2)
                    return false;
                String kitName = args[1];
                if (gameManager.getKitManager().kitExists(kitName)) {
                    gameManager.setSelectedKit(kitName);
                    sender.sendMessage(
                            Component.text("Kit für das Match auf '" + kitName + "' gesetzt.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Kit '" + kitName + "' existiert nicht!", NamedTextColor.RED));
                }
                break;
            case "deletekit":
                if (args.length < 2)
                    return false;
                String delKit = args[1];
                if (gameManager.getKitManager().deleteKit(delKit)) {
                    if (delKit.equals(gameManager.getSelectedKit())) {
                        gameManager.setSelectedKit(null);
                    }
                    sender.sendMessage(Component.text("Kit '" + delKit + "' wurde gelöscht.", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(
                            Component.text("Kit '" + delKit + "' konnte nicht gefunden werden.", NamedTextColor.RED));
                }
                break;
            case "toggledrops":
                boolean newState = !gameManager.isDropItems();
                gameManager.setDropItems(newState);
                sender.sendMessage(Component.text("Item Drops beim Tod: " + (newState ? "AN" : "AUS"),
                        newState ? NamedTextColor.GREEN : NamedTextColor.RED));
                break;
            case "start":
                gameManager.startMatch();
                break;
            case "stop":
                gameManager.stopMatch();
                break;
            case "resetleaderboard":
                pointManager.resetPoints();
                sender.sendMessage(Component.text("Leaderboard zurückgesetzt.", NamedTextColor.GREEN));
                break;
            case "points":
                pointManager.displayLeaderboard();
                break;
            case "history":
                pointManager.showHistory(sender);
                break;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(Component.text("Konfiguration neu geladen.", NamedTextColor.GREEN));
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("setsolo", "addattacker", "removeattacker", "rounds",
                    "savekit", "setkit", "deletekit", "toggledrops", "start", "stop", "resetleaderboard", "points",
                    "history", "reload");
            StringUtil.copyPartialMatches(args[0], subCommands, completions);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setsolo") || sub.equals("addattacker") || sub.equals("removeattacker")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    players.add(p.getName());
                }
                StringUtil.copyPartialMatches(args[1], players, completions);
            } else if (sub.equals("setkit") || sub.equals("deletekit")) {
                StringUtil.copyPartialMatches(args[1], gameManager.getKitManager().getKitNames(), completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("savekit")) {
            StringUtil.copyPartialMatches(args[2], List.of("regen"), completions);
        }
        Collections.sort(completions);
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- 1vs5 Admin ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/1vs5 setsolo <Name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 addattacker <Name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 removeattacker <Name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 rounds <Anzahl>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 savekit <Name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 setkit <Name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 deletekit <Name>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 toggledrops", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 start", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 stop", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 resetleaderboard", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 points", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 history", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/1vs5 reload", NamedTextColor.YELLOW));
    }
}
