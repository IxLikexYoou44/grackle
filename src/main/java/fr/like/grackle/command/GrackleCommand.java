package fr.like.grackle.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.like.grackle.manager.EventManager;
import fr.like.grackle.manager.GameManager;
import fr.like.grackle.model.event.Event;
import fr.like.grackle.model.game.Game;
import fr.like.grackle.model.game.GameParticipant;
import fr.like.grackle.model.game.GameStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /grackle start <eventName>          – crée et démarre une partie
 * /grackle join <gameId>              – ajoute le joueur à la partie
 * /grackle leave <gameId>             – retire le joueur
 * /grackle refreshTeams <gameId>      – lu par command block, lit les tags et affecte les équipes
 * /grackle startGame <gameId>         – passe en IN_GAME
 * /grackle end <gameId>               – distribue les récompenses et termine
 * /grackle setPosition <gameId> <joueur|équipe> <position> – définit le classement final
 * /grackle status <gameId>            – état de la partie
 */
public class GrackleCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final EventManager eventManager;
    private final GameManager gameManager;

    public GrackleCommand(ProxyServer proxy, EventManager eventManager, GameManager gameManager) {
        this.proxy = proxy;
        this.eventManager = eventManager;
        this.gameManager = gameManager;
    }

    @Override
    public void execute(Invocation inv) {
        CommandSource src = inv.source();
        String[] args = inv.arguments();

        if (args.length == 0) { sendHelp(src); return; }

        switch (args[0].toLowerCase()) {
            case "start"        -> handleStart(src, args);
            case "join"         -> handleJoin(src, args);
            case "leave"        -> handleLeave(src, args);
            case "refreshteams" -> handleRefreshTeams(src, args);
            case "startgame"    -> handleStartGame(src, args);
            case "end"          -> handleEnd(src, args);
            case "setposition"  -> handleSetPosition(src, args);
            case "status"       -> handleStatus(src, args);
            default             -> src.sendMessage(Component.text("Sous-commande inconnue.", NamedTextColor.RED));
        }
    }

    // -------------------------------------------------------------------------

    private void handleStart(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(usage("start <eventName>")); return; }
        eventManager.get(args[1]).ifPresentOrElse(event -> {
            Game game = gameManager.createGame(event);
            src.sendMessage(Component.text("Partie #" + game.getId() + " créée pour '" + event.getName() + "'.", NamedTextColor.GREEN));
        }, () -> src.sendMessage(Component.text("Event introuvable.", NamedTextColor.RED)));
    }

    private void handleJoin(CommandSource src, String[] args) {
        if (!(src instanceof com.velocitypowered.api.proxy.Player player)) {
            src.sendMessage(Component.text("Seul un joueur peut rejoindre une partie.", NamedTextColor.RED)); return;
        }
        if (args.length < 2) { src.sendMessage(usage("join <gameId>")); return; }
        int id = parseInt(args[1]);
        gameManager.getGame(id).ifPresentOrElse(game -> {
            if (game.getStatus() != GameStatus.WAITING) {
                src.sendMessage(Component.text("La partie n'accepte plus de joueurs.", NamedTextColor.RED)); return;
            }
            if (game.hasParticipant(player.getUniqueId())) {
                src.sendMessage(Component.text("Tu es déjà dans cette partie.", NamedTextColor.YELLOW)); return;
            }
            if (game.getParticipantCount() >= game.getEvent().getMaxParticipants()) {
                src.sendMessage(Component.text("La partie est complète.", NamedTextColor.RED)); return;
            }
            game.addParticipant(new GameParticipant(player.getUniqueId(), player.getUsername()));
            src.sendMessage(Component.text("Tu as rejoint la partie #" + id + ".", NamedTextColor.GREEN));
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    private void handleLeave(CommandSource src, String[] args) {
        if (!(src instanceof com.velocitypowered.api.proxy.Player player)) {
            src.sendMessage(Component.text("Seul un joueur peut quitter une partie.", NamedTextColor.RED)); return;
        }
        if (args.length < 2) { src.sendMessage(usage("leave <gameId>")); return; }
        int id = parseInt(args[1]);
        gameManager.getGame(id).ifPresentOrElse(game -> {
            if (game.removeParticipant(player.getUniqueId())) {
                src.sendMessage(Component.text("Tu as quitté la partie #" + id + ".", NamedTextColor.GREEN));
            } else {
                src.sendMessage(Component.text("Tu n'es pas dans cette partie.", NamedTextColor.RED));
            }
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    private void handleRefreshTeams(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(usage("refreshTeams <gameId>")); return; }
        int id = parseInt(args[1]);
        gameManager.getGame(id).ifPresentOrElse(game -> {
            if (game.getStatus() != GameStatus.REFRESH_TEAM) {
                game.setStatus(GameStatus.REFRESH_TEAM);
            }
            gameManager.refreshTeams(game, proxy.getAllPlayers());
            src.sendMessage(Component.text("Équipes rafraîchies pour la partie #" + id + ".", NamedTextColor.GREEN));
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    private void handleStartGame(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(usage("startGame <gameId>")); return; }
        int id = parseInt(args[1]);
        gameManager.getGame(id).ifPresentOrElse(game -> {
            if (!game.canStart()) {
                src.sendMessage(Component.text("Conditions non remplies (participants: " + game.getParticipantCount()
                        + ", min: " + game.getEvent().getMinParticipants() + ").", NamedTextColor.RED));
                return;
            }
            game.setStatus(GameStatus.IN_GAME);
            src.sendMessage(Component.text("Partie #" + id + " lancée !", NamedTextColor.GREEN));
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    private void handleEnd(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(usage("end <gameId>")); return; }
        int id = parseInt(args[1]);
        gameManager.getGame(id).ifPresentOrElse(game -> {
            Map<UUID, Integer> rewards = gameManager.endGame(game);
            src.sendMessage(Component.text("Partie #" + id + " terminée. Récompenses :", NamedTextColor.GOLD));
            rewards.forEach((uuid, pts) -> {
                proxy.getPlayer(uuid).ifPresent(p ->
                        src.sendMessage(Component.text("  " + p.getUsername() + " → +" + pts + " pts", NamedTextColor.AQUA)));
            });
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    private void handleSetPosition(CommandSource src, String[] args) {
        // /grackle setPosition <gameId> <playerName|teamName> <position>
        if (args.length < 4) { src.sendMessage(usage("setPosition <gameId> <nom> <position>")); return; }
        int id = parseInt(args[1]);
        String target = args[2];
        int pos = parseInt(args[3]);

        gameManager.getGame(id).ifPresentOrElse(game -> {
            // Essai équipe d'abord
            game.getTeam(target).ifPresentOrElse(team -> {
                team.setFinalPosition(pos);
                src.sendMessage(Component.text("Équipe '" + target + "' → position " + pos + ".", NamedTextColor.GREEN));
            }, () -> {
                // Essai joueur
                proxy.getPlayer(target).ifPresentOrElse(p -> {
                    GameParticipant gp = game.getParticipants().get(p.getUniqueId());
                    if (gp == null) { src.sendMessage(Component.text("Joueur non trouvé dans la partie.", NamedTextColor.RED)); return; }
                    gp.setFinalPosition(pos);
                    src.sendMessage(Component.text(p.getUsername() + " → position " + pos + ".", NamedTextColor.GREEN));
                }, () -> src.sendMessage(Component.text("Joueur/équipe introuvable.", NamedTextColor.RED)));
            });
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    private void handleStatus(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(usage("status <gameId>")); return; }
        int id = parseInt(args[1]);
        gameManager.getGame(id).ifPresentOrElse(game -> {
            src.sendMessage(Component.text("=== Partie #" + id + " ===", NamedTextColor.GOLD));
            src.sendMessage(Component.text("Event : " + game.getEvent().getName(), NamedTextColor.WHITE));
            src.sendMessage(Component.text("Statut : " + game.getStatus(), NamedTextColor.WHITE));
            src.sendMessage(Component.text("Joueurs : " + game.getParticipantCount(), NamedTextColor.WHITE));
            if (!game.getTeams().isEmpty()) {
                game.getTeams().values().forEach(t ->
                        src.sendMessage(Component.text("  Équipe " + t.getName() + " : " + t.getMembers().size() + " joueur(s)", NamedTextColor.AQUA)));
            }
        }, () -> src.sendMessage(Component.text("Partie introuvable.", NamedTextColor.RED)));
    }

    // -------------------------------------------------------------------------

    private Component usage(String s) { return Component.text("Usage: /grackle " + s, NamedTextColor.RED); }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return -1; }
    }

    private void sendHelp(CommandSource src) {
        List.of(
                "/grackle start <eventName>",
                "/grackle join <gameId>",
                "/grackle leave <gameId>",
                "/grackle refreshTeams <gameId>",
                "/grackle startGame <gameId>",
                "/grackle end <gameId>",
                "/grackle setPosition <gameId> <nom> <position>",
                "/grackle status <gameId>"
        ).forEach(s -> src.sendMessage(Component.text(s, NamedTextColor.YELLOW)));
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        return inv.source().hasPermission("grackle.admin");
    }
}
