package therohero.onevsfive.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {
    private final GameManager gameManager;
    private final PointManager pointManager;

    public ScoreboardManager(GameManager gameManager, PointManager pointManager) {
        this.gameManager = gameManager;
        this.pointManager = pointManager;
    }

    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = scoreboard.registerNewObjective("1vs5", Criteria.DUMMY,
                Component.text("1vs5 EVENT", NamedTextColor.GOLD, TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = new ArrayList<>();
        lines.add("§7----------------");
        lines.add("§fRunde: §e" + gameManager.getCurrentRound() + "§7/§e" + gameManager.getTotalRounds());

        if (gameManager.isGameRunning()) {
            lines.add("§fStatus: §aIm Kampf");
            lines.add("§fÜberlebende: §b" + gameManager.getAliveAttackersCount());
        } else if (gameManager.isCountingDown()) {
            lines.add("§fStatus: §6Countdown...");
        } else {
            lines.add("§fStatus: §7Lobby");
        }

        lines.add(" ");
        lines.add("§6Punkte:");

        Map<UUID, Integer> points = new HashMap<>(pointManager.getPoints());
        points.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(entry -> {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    if (name != null) {
                        lines.add("§f" + name + ": §e" + entry.getValue());
                    }
                });

        lines.add("§7---------------- ");

        for (int i = 0; i < lines.size(); i++) {
            obj.getScore(lines.get(i)).setScore(lines.size() - i);
        }

        player.setScoreboard(scoreboard);
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }
}
