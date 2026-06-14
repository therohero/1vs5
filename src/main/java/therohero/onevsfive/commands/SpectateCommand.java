package therohero.onevsfive.commands;

import therohero.onevsfive.managers.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpectateCommand implements CommandExecutor {
    private final GameManager gameManager;

    public SpectateCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen!");
            return true;
        }

        if (gameManager.isGameRunning() && player.getWorld().getName().equals("fightworld") &&
                (player.getUniqueId().equals(gameManager.getSoloPlayer())
                        || gameManager.getAttackers().contains(player.getUniqueId()))) {
            player.sendMessage(net.kyori.adventure.text.Component.text(
                    "Du kannst nicht zuschauen, während du aktiv am Kampf teilnimmst!",
                    net.kyori.adventure.text.format.NamedTextColor.RED));
            return true;
        }

        gameManager.spectateFight(player);
        return true;
    }
}
