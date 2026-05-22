package fr.like.grackle.manager;

import fr.like.grackle.model.event.Event;
import fr.like.grackle.model.event.EventType;
import fr.like.grackle.model.event.SoloMode;
import fr.like.grackle.model.event.TeamConfig;
import fr.like.grackle.model.game.*;
import org.slf4j.Logger;

import java.util.*;

public class GameManager {

    private final Logger logger;
    private final Map<Integer, Game> activeGames = new HashMap<>();

    public GameManager(Logger logger) {
        this.logger = logger;
    }

    public Game createGame(Event event) {
        Game game = new Game(event);

        if (event.getType() == EventType.TEAM && event.getTeamConfig() != null) {
            TeamConfig cfg = event.getTeamConfig();
            cfg.getTeamTags().values().stream()
                    .distinct()
                    .forEach(teamName -> game.addTeam(new GameTeam(teamName)));
        }

        activeGames.put(game.getId(), game);
        logger.info("Partie #{} créée pour l'event '{}'", game.getId(), event.getName());
        return game;
    }

    public Optional<Game> getGame(int id) {
        return Optional.ofNullable(activeGames.get(id));
    }

    public Collection<Game> getActiveGames() {
        return activeGames.values();
    }

    /**
     * Lit les tags Velocity du joueur et l'affecte à l'équipe correspondante.
     * Appelé via /grackle refreshTeams <gameId> (déclenché par command block).
     */
    public void refreshTeams(Game game, Collection<com.velocitypowered.api.proxy.Player> players) {
        if (game.getStatus() != GameStatus.REFRESH_TEAM) return;

        TeamConfig cfg = game.getEvent().getTeamConfig();
        Map<String, String> teamTags = cfg.getTeamTags(); // tag → teamName

        for (com.velocitypowered.api.proxy.Player player : players) {
            if (!game.hasParticipant(player.getUniqueId())) continue;

            game.getTeams().values().forEach(t -> t.removeMember(player.getUniqueId()));

            for (Map.Entry<String, String> entry : teamTags.entrySet()) {
                String tag = entry.getKey();
                String teamName = entry.getValue();

                if (playerHasTag(player, tag)) {
                    game.getTeam(teamName).ifPresent(t -> {
                        t.addMember(player.getUniqueId());
                        logger.debug("{} → équipe {}", player.getUsername(), teamName);
                    });
                    break;
                }
            }
        }
    }

    /**
     * Distribue les récompenses et passe la partie en ENDED.
     * Solo RANKED  : position définie par finalPosition sur GameParticipant.
     * Solo PARTICIPATION : tous reçoivent la récompense de position 1.
     * TEAM        : position définie par finalPosition sur GameTeam.
     */
    public Map<UUID, Integer> endGame(Game game) {
        game.setStatus(GameStatus.ENDED);
        Map<UUID, Integer> rewards = new HashMap<>();
        Event event = game.getEvent();

        if (event.getType() == EventType.SOLO) {
            if (event.getSoloMode() == SoloMode.PARTICIPATION) {
                game.getParticipants().values().forEach(p -> {
                    int pts = event.getRewardStrategy().getPoints(1);
                    p.setPointsEarned(pts);
                    rewards.put(p.getUuid(), pts);
                });
            } else {
                game.getParticipants().values().forEach(p -> {
                    if (p.getFinalPosition() < 1) return;
                    int pts = event.getRewardStrategy().getPoints(p.getFinalPosition());
                    p.setPointsEarned(pts);
                    rewards.put(p.getUuid(), pts);
                });
            }
        } else {
            game.getTeams().values().forEach(team -> {
                if (team.getFinalPosition() < 1) return;
                int pts = event.getRewardStrategy().getPoints(team.getFinalPosition());
                team.getMembers().forEach(uuid -> {
                    game.getParticipants().computeIfPresent(uuid, (k, p) -> {
                        p.setPointsEarned(pts);
                        return p;
                    });
                    rewards.put(uuid, pts);
                });
            });
        }

        activeGames.remove(game.getId());
        logger.info("Partie #{} terminée. {} récompenses distribuées.", game.getId(), rewards.size());
        return rewards;
    }

    // -------------------------------------------------------------------------

    private boolean playerHasTag(com.velocitypowered.api.proxy.Player player, String tag) {
        // Velocity n'a pas d'API de tags natifs — on délègue à LuckPerms ou autre.
        // Cette méthode sera implémentée via l'intégration LuckPerms.
        return false;
    }
}
