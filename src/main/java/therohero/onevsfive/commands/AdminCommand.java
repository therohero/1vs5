package therohero.onevsfive.commands;

import therohero.onevsfive.managers.GameManager;
import therohero.onevsfive.managers.LangManager;
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
    private final LangManager lang;

    public AdminCommand(org.bukkit.plugin.Plugin plugin, GameManager gameManager, PointManager pointManager,
            LangManager lang) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.pointManager = pointManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text(lang.get("no_permission"), NamedTextColor.RED));
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
                    sender.sendMessage(Component.text(lang.get("cmd_set_solo", solo.getName()), NamedTextColor.GREEN));
                }
                break;
            case "addattacker":
                if (args.length < 2)
                    return false;
                Player att = Bukkit.getPlayer(args[1]);
                if (att != null) {
                    gameManager.addAttacker(att.getUniqueId());
                    sender.sendMessage(
                            Component.text(lang.get("cmd_add_attacker", att.getName()), NamedTextColor.GREEN));
                }
                break;
            case "removeattacker":
                if (args.length < 2)
                    return false;
                Player rem = Bukkit.getPlayer(args[1]);
                if (rem != null) {
                    gameManager.removeAttacker(rem.getUniqueId());
                    sender.sendMessage(
                            Component.text(lang.get("cmd_remove_attacker", rem.getName()), NamedTextColor.YELLOW));
                }
                break;
            case "rounds":
                if (args.length < 2)
                    return false;
                try {
                    int r = Integer.parseInt(args[1]);
                    gameManager.setTotalRounds(r);
                    sender.sendMessage(Component.text(lang.get("cmd_set_rounds", r), NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text(lang.get("cmd_invalid_number"), NamedTextColor.RED));
                }
                break;
            case "savekit":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text(lang.get("cmd_players_only_kit"), NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2)
                    return false;
                try {
                    // Standardmäßig true, außer das 3. Argument ist explizit "false"
                    boolean naturalRegen = true;
                    if (args.length >= 3) {
                        if (args[2].equalsIgnoreCase("false")) {
                            naturalRegen = false;
                        }
                    }

                    gameManager.getKitManager().saveKit(args[1], player, naturalRegen);

                    // Nachricht anpassen (Du kannst deine Sprachdateien entsprechend erweitern)
                    String msg = naturalRegen
                            ? lang.get("cmd_kit_saved_regen", args[1]) // oder cmd_kit_saved
                            : lang.get("cmd_kit_saved_no_regen", args[1]);

                    sender.sendMessage(Component.text(msg, NamedTextColor.GREEN));
                } catch (Exception e) {
                    sender.sendMessage(Component.text(lang.get("cmd_kit_save_error"), NamedTextColor.RED));
                    e.printStackTrace();
                }
                break;
            case "setkit":
                if (args.length < 2)
                    return false;
                String kitName = args[1];
                if (gameManager.getKitManager().kitExists(kitName)) {
                    gameManager.setSelectedKit(kitName);
                    sender.sendMessage(Component.text(lang.get("cmd_kit_set", kitName), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text(lang.get("cmd_kit_not_found", kitName), NamedTextColor.RED));
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
                    sender.sendMessage(Component.text(lang.get("cmd_kit_deleted", delKit), NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(
                            Component.text(lang.get("cmd_kit_delete_not_found", delKit), NamedTextColor.RED));
                }
                break;
            case "toggledrops":
                boolean newState = !gameManager.isDropItems();
                gameManager.setDropItems(newState);
                sender.sendMessage(Component.text(
                        newState ? lang.get("cmd_drops_on") : lang.get("cmd_drops_off"),
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
                sender.sendMessage(Component.text(lang.get("cmd_leaderboard_reset"), NamedTextColor.GREEN));
                break;
            case "points":
                pointManager.displayLeaderboard();
                break;
            case "history":
                pointManager.showHistory(sender);
                break;
            case "reload":
                plugin.reloadConfig();
                lang.reload();
                sender.sendMessage(Component.text(lang.get("cmd_config_reloaded"), NamedTextColor.GREEN));
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
        sender.sendMessage(Component.text(lang.get("help_header"), NamedTextColor.GOLD));
        sender.sendMessage(Component.text(lang.get("help_setsolo"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_addattacker"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_removeattacker"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_rounds"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_savekit"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_setkit"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_deletekit"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_toggledrops"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_start"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_stop"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_resetleaderboard"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_points"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_history"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(lang.get("help_reload"), NamedTextColor.YELLOW));
    }
}