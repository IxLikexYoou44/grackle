package fr.like.grackle.model.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameTeam {

    private final String name;
    private final List<UUID> members = new ArrayList<>();
    private int finalPosition = -1; // -1 = non défini

    public GameTeam(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public List<UUID> getMembers() { return members; }

    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }

    public int getFinalPosition() { return finalPosition; }
    public void setFinalPosition(int finalPosition) { this.finalPosition = finalPosition; }
}
