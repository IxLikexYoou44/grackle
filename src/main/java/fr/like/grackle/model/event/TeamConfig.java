package fr.like.grackle.model.event;

import java.util.HashMap;
import java.util.Map;

public class TeamConfig {

    private final int teamCount;
    private final boolean randomTeams;
    /** teamName → exact size (null = no constraint). Used for special roles like PropHunt hunter. */
    private final Map<String, Integer> fixedTeamSizes;
    /** Tags that identify each team: tagValue → teamName */
    private final Map<String, String> teamTags;

    public TeamConfig(int teamCount, boolean randomTeams,
                      Map<String, Integer> fixedTeamSizes,
                      Map<String, String> teamTags) {
        this.teamCount = teamCount;
        this.randomTeams = randomTeams;
        this.fixedTeamSizes = new HashMap<>(fixedTeamSizes);
        this.teamTags = new HashMap<>(teamTags);
    }

    public int getTeamCount() { return teamCount; }
    public boolean isRandomTeams() { return randomTeams; }
    public Map<String, Integer> getFixedTeamSizes() { return fixedTeamSizes; }
    public Map<String, String> getTeamTags() { return teamTags; }
}
