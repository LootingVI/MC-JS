package de.flori.mCJS.api;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.plugin.java.JavaPlugin;

public class ScoreboardAPI extends BaseAPI {

    public ScoreboardAPI(JavaPlugin plugin) {
        super(plugin);
    }

    public Scoreboard getMainScoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }

    public Scoreboard createScoreboard() {
        return Bukkit.getScoreboardManager().getNewScoreboard();
    }

    public Team getTeam(Scoreboard scoreboard, String name) {
        return scoreboard.getTeam(name);
    }

    public Team createTeam(Scoreboard scoreboard, String name) {
        return scoreboard.registerNewTeam(name);
    }

    public Objective getObjective(Scoreboard scoreboard, String name) {
        return scoreboard.getObjective(name);
    }

    public Objective createObjective(Scoreboard scoreboard, String name, String criteria, String displayName) {
        Component component = legacyToComponentWithAmpersand(displayName);
        @SuppressWarnings("deprecation")
        Objective objective = scoreboard.registerNewObjective(name, criteria, component);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        return objective;
    }
}
