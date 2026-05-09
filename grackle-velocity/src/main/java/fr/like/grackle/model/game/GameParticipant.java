package fr.like.grackle.model.game;

import java.util.UUID;

public class GameParticipant {

    private final UUID uuid;
    private final String username;
    private int finalPosition = -1; // solo uniquement
    private int pointsEarned = 0;

    public GameParticipant(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }

    public int getFinalPosition() { return finalPosition; }
    public void setFinalPosition(int finalPosition) { this.finalPosition = finalPosition; }

    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
}
