package fr.like.grackle.model.game;

import fr.like.grackle.model.event.Event;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Game {

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private final int id;
    private final Event event;
    private GameStatus status;

    private final Map<UUID, GameParticipant> participants = new LinkedHashMap<>();
    private final Map<String, GameTeam> teams = new LinkedHashMap<>();

    public Game(Event event) {
        this.id = idCounter.incrementAndGet();
        this.event = event;
        this.status = GameStatus.WAITING;
    }

    public int getId() { return id; }
    public Event getEvent() { return event; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public Map<UUID, GameParticipant> getParticipants() { return participants; }

    public void addParticipant(GameParticipant p) {
        participants.put(p.getUuid(), p);
    }

    public boolean removeParticipant(UUID uuid) {
        return participants.remove(uuid) != null;
    }

    public boolean hasParticipant(UUID uuid) {
        return participants.containsKey(uuid);
    }

    public Map<String, GameTeam> getTeams() { return teams; }

    public void addTeam(GameTeam team) {
        teams.put(team.getName().toLowerCase(), team);
    }

    public Optional<GameTeam> getTeam(String name) {
        return Optional.ofNullable(teams.get(name.toLowerCase()));
    }

    /** Returns the team a player currently belongs to, if any. */
    public Optional<GameTeam> getTeamOf(UUID uuid) {
        return teams.values().stream()
                .filter(t -> t.getMembers().contains(uuid))
                .findFirst();
    }

    public int getParticipantCount() { return participants.size(); }

    public boolean canStart() {
        int count = getParticipantCount();
        return count >= event.getMinParticipants() && count <= event.getMaxParticipants();
    }
}
