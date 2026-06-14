package therohero.onevsfive.commands;

import therohero.onevsfive.managers.GameManager;
import therohero.onevsfive.managers.LangManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpectateCommand implements CommandExecutor {
    private final GameManager gameManager;
    private final LangManager lang;

    public SpectateCommand(GameManager gameManager, LangManager lang) {
        this.gameManager = gameManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.get("players_only"));
            return true;
        }

        if (gameManager.isGameRunning() && player.getWorld().getName().equals("fightworld") &&
                (player.getUniqueId().equals(gameManager.getSoloPlayer())
                        || gameManager.getAttackers().contains(player.getUniqueId()))) {
            player.sendMessage(Component.text(lang.get("spectate_not_allowed"), NamedTextColor.RED));
            return true;
        }

        gameManager.spectateFight(player);
        return true;
    }
}