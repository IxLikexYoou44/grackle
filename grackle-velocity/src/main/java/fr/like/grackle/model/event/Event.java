package fr.like.grackle.model.event;

import fr.like.grackle.model.reward.RewardStrategy;

public class Event {

    private final String name;
    private final EventType type;

    // Solo only
    private SoloMode soloMode;

    // Team only
    private TeamConfig teamConfig;

    private RewardStrategy rewardStrategy;
    private Category category;

    private String description;

    private int minParticipants;
    private int maxParticipants;

    public Event(String name, EventType type) {
        this.name = name;
        this.type = type;
        this.minParticipants = 2;
        this.maxParticipants = Integer.MAX_VALUE;
    }

    public String getName() { return name; }
    public EventType getType() { return type; }

    public SoloMode getSoloMode() { return soloMode; }
    public void setSoloMode(SoloMode soloMode) { this.soloMode = soloMode; }

    public TeamConfig getTeamConfig() { return teamConfig; }
    public void setTeamConfig(TeamConfig teamConfig) { this.teamConfig = teamConfig; }

    public RewardStrategy getRewardStrategy() { return rewardStrategy; }
    public void setRewardStrategy(RewardStrategy rewardStrategy) { this.rewardStrategy = rewardStrategy; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getMinParticipants() { return minParticipants; }
    public void setMinParticipants(int minParticipants) { this.minParticipants = minParticipants; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
}
