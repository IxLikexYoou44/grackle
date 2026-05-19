package fr.like.grackle.messaging;

import com.google.gson.*;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.like.grackle.Grackle;
import fr.like.grackle.manager.CategoryManager;
import fr.like.grackle.manager.EventManager;
import fr.like.grackle.manager.GameManager;
import fr.like.grackle.model.event.Event;
import fr.like.grackle.model.event.EventType;
import fr.like.grackle.model.event.SoloMode;
import fr.like.grackle.model.event.TeamConfig;
import fr.like.grackle.model.game.Game;
import fr.like.grackle.model.game.GameParticipant;
import fr.like.grackle.model.game.GameStatus;
import fr.like.grackle.model.reward.DegressiveReward;
import fr.like.grackle.model.reward.TeamReward;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class VelocityMessagingHandler {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("grackle:main");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Grackle plugin;
    private final EventManager eventManager;
    private final CategoryManager categoryManager;
    private final GameManager gameManager;
    private final Gson gson = new GsonBuilder().create();

    public VelocityMessagingHandler(ProxyServer proxy, Logger logger, Grackle plugin,
                                    EventManager eventManager, CategoryManager categoryManager,
                                    GameManager gameManager) {
        this.proxy = proxy;
        this.logger = logger;
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.categoryManager = categoryManager;
        this.gameManager = gameManager;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) return;
        if (!(event.getSource() instanceof ServerConnection server)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String raw = new String(event.getData(), StandardCharsets.UTF_8);
        JsonObject msg;
        try {
            msg = gson.fromJson(raw, JsonObject.class);
        } catch (JsonParseException e) {
            logger.warn("Message plugin malformé : {}", raw);
            return;
        }

        MessageType type = MessageType.valueOf(msg.get("type").getAsString());
        UUID playerId = UUID.fromString(msg.get("playerId").getAsString());

        switch (type) {
            case REQUEST_STATE -> sendState(server, playerId);
            case ACTION        -> handleAction(server, playerId, msg);
            default -> logger.warn("Type de message inattendu depuis Paper : {}", type);
        }
    }

    // -------------------------------------------------------------------------

    private void sendState(ServerConnection server, UUID playerId) {
        JsonObject resp = new JsonObject();
        resp.addProperty("type", MessageType.STATE_RESPONSE.name());
        resp.addProperty("playerId", playerId.toString());
        resp.add("events", buildEventsArray());
        resp.add("categories", buildCategoriesArray());
        resp.add("games", buildGamesArray());
        send(server, resp);
    }

    private void handleAction(ServerConnection server, UUID playerId, JsonObject msg) {
        String action = msg.get("action").getAsString();
        JsonObject params = msg.has("params") ? msg.getAsJsonObject("params") : new JsonObject();

        boolean success;
        String message;
        String responseTargetServer = null;

        switch (action) {
            case "START_GAME" -> {
                String eventName = params.has("event") ? params.get("event").getAsString() : "";
                Optional<Event> ev = eventManager.get(eventName);
                if (ev.isEmpty()) { replyError(server, playerId, "Event introuvable."); return; }
                Event event = ev.get();
                Game game = gameManager.createGame(event);

                // If a target server is provided in params, attempt to forward the initiating player
                if (params.has("targetServer") && !params.get("targetServer").isJsonNull()) {
                    String target = params.get("targetServer").getAsString();
                    // store for response
                    responseTargetServer = target;
                    proxy.getServer(target).ifPresentOrElse(reg -> {
                        proxy.getPlayer(playerId).ifPresent(p -> {
                            try {
                                p.createConnectionRequest(reg).connect();
                                logger.info("Forwarded player {} to server {} for game #{}", playerId, target, game.getId());
                            } catch (Exception ex) {
                                logger.warn("Failed to forward player {} to server {}: {}", playerId, target, ex.getMessage());
                            }
                        });
                    }, () -> logger.warn("Target server '{}' not found for START_GAME", target));
                }

                plugin.save();
                success = true;
                message = "Partie #" + game.getId() + " créée.";
            }
            case "JOIN_GAME" -> {
                int gameId = params.get("gameId").getAsInt();
                Optional<Game> g = gameManager.getGame(gameId);
                if (g.isEmpty()) { replyError(server, playerId, "Partie introuvable."); return; }
                Game game = g.get();
                if (game.getStatus() != GameStatus.WAITING) { replyError(server, playerId, "La partie n'accepte plus de joueurs."); return; }
                if (game.getParticipantCount() >= game.getEvent().getMaxParticipants()) { replyError(server, playerId, "Partie complète."); return; }
                if (proxy.getPlayer(playerId).isPresent()) {
                    proxy.getPlayer(playerId).ifPresent(p ->
                            game.addParticipant(new GameParticipant(playerId, p.getUsername())));
                    success = true;
                    message = "Tu as rejoint la partie #" + gameId + ".";
                } else {
                    replyError(server, playerId, "Joueur introuvable sur le proxy.");
                    return;
                }
            }
            case "LEAVE_GAME" -> {
                int gameId = params.get("gameId").getAsInt();
                Optional<Game> g = gameManager.getGame(gameId);
                if (g.isEmpty()) { replyError(server, playerId, "Partie introuvable."); return; }
                success = g.get().removeParticipant(playerId);
                message = success ? "Tu as quitté la partie #" + gameId + "." : "Tu n'es pas dans cette partie.";
            }
            case "END_GAME" -> {
                int gameId = params.get("gameId").getAsInt();
                Optional<Game> g = gameManager.getGame(gameId);
                if (g.isEmpty()) { replyError(server, playerId, "Partie introuvable."); return; }
                gameManager.endGame(g.get());
                success = true;
                message = "Partie #" + gameId + " terminée.";
            }
            case "REFRESH_TEAMS" -> {
                int gameId = params.get("gameId").getAsInt();
                Optional<Game> g = gameManager.getGame(gameId);
                if (g.isEmpty()) { replyError(server, playerId, "Partie introuvable."); return; }
                Game game = g.get();
                game.setStatus(GameStatus.REFRESH_TEAM);
                gameManager.refreshTeams(game, proxy.getAllPlayers());
                success = true;
                message = "Équipes rafraîchies.";
            }
            case "CATEGORY_CREATE" -> {
                String name = params.has("name") ? params.get("name").getAsString() : "";
                String desc = params.has("description") ? params.get("description").getAsString() : "";
                if (name.isEmpty()) { replyError(server, playerId, "Nom de catégorie manquant."); return; }
                if (categoryManager.exists(name)) { replyError(server, playerId, "La catégorie existe déjà."); return; }
                fr.like.grackle.model.event.Category c = categoryManager.create(name);
                if (!desc.isEmpty()) c.setDescription(desc);
                plugin.save();
                success = true;
                message = "Catégorie '" + name + "' créée.";
            }
            case "CATEGORY_DELETE" -> {
                String name = params.has("name") ? params.get("name").getAsString() : "";
                if (name.isEmpty()) { replyError(server, playerId, "Nom de catégorie manquant."); return; }
                boolean removed = categoryManager.remove(name);
                if (!removed) { replyError(server, playerId, "Catégorie introuvable."); return; }
                plugin.save();
                success = true;
                message = "Catégorie '" + name + "' supprimée.";
            }
            case "EVENT_CREATE" -> {
                String name = params.has("name") ? params.get("name").getAsString() : "";
                String typeStr = params.has("type") ? params.get("type").getAsString() : "";
                List<String> options = new ArrayList<>();
                if (params.has("options") && params.get("options").isJsonArray()) {
                    params.getAsJsonArray("options").forEach(el -> options.add(el.getAsString()));
                }
                if (name.isEmpty() || typeStr.isEmpty()) { replyError(server, playerId, "Nom/type manquant pour l'event."); return; }
                EventType type;
                try { type = EventType.valueOf(typeStr.toUpperCase()); } catch (IllegalArgumentException e) { replyError(server, playerId, "Type d'event invalide."); return; }
                if (eventManager.get(name).isPresent()) { replyError(server, playerId, "Un event avec ce nom existe déjà."); return; }
                Event event = new Event(name, type);
                // Parse options similarly à EventCommand
                try {
                    parseOptionsForEvent(event, options.toArray(new String[0]));
                } catch (Exception e) {
                    replyError(server, playerId, "Erreur lors du parsing des options: " + e.getMessage());
                    return;
                }
                // Defaults
                if (event.getRewardStrategy() == null) {
                    if (type == EventType.TEAM) event.setRewardStrategy(new TeamReward(3,1));
                    else event.setRewardStrategy(new DegressiveReward(5,1,1));
                }
                if (type == EventType.SOLO && event.getSoloMode() == null) event.setSoloMode(SoloMode.RANKED);
                if (type == EventType.TEAM && event.getTeamConfig() == null) event.setTeamConfig(new TeamConfig(2, false, Map.of(), Map.of()));

                eventManager.register(event);
                plugin.save();
                success = true;
                message = "Event '" + name + "' créé.";
            }
            case "EVENT_DELETE" -> {
                String name = params.has("name") ? params.get("name").getAsString() : "";
                if (name.isEmpty()) { replyError(server, playerId, "Nom d'event manquant."); return; }
                if (!eventManager.remove(name)) { replyError(server, playerId, "Event introuvable."); return; }
                plugin.save();
                success = true;
                message = "Event '" + name + "' supprimé.";
            }
            default -> { replyError(server, playerId, "Action inconnue : " + action); return; }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("type", MessageType.ACTION_RESULT.name());
        resp.addProperty("playerId", playerId.toString());
        resp.addProperty("success", success);
        resp.addProperty("message", message);
        if (responseTargetServer != null) resp.addProperty("targetServer", responseTargetServer);
        resp.add("games", buildGamesArray());
        send(server, resp);
        pushStateToAll();
    }

    /**
     * Parse basic options for events (copié et adapté depuis EventCommand.parseOptions).
     */
    private void parseOptionsForEvent(Event event, String[] args) {
        int teamCount = 2;
        boolean randomTeams = false;
        int rewardMax = -1, rewardRange = -1, rewardParticipation = 1;
        int winner = -1, loser = -1;

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--mode"         -> { if (++i < args.length) event.setSoloMode(SoloMode.valueOf(args[i].toUpperCase())); }
                case "--min"          -> { if (++i < args.length) event.setMinParticipants(Integer.parseInt(args[i])); }
                case "--max-players", "--max"  -> { if (++i < args.length) event.setMaxParticipants(args[i].equals("-1") ? Integer.MAX_VALUE : Integer.parseInt(args[i])); }
                case "--category"     -> { if (++i < args.length) categoryManager.get(args[i]).ifPresent(event::setCategory); }
                case "--desc"         -> { if (++i < args.length) event.setDescription(args[i]); }
                case "--server"       -> { if (++i < args.length) event.setServer(args[i]); }
                case "--teams"        -> { if (++i < args.length) teamCount = Integer.parseInt(args[i]); }
                case "--random"       -> randomTeams = true;
                case "--range"        -> { if (++i < args.length) rewardRange = Integer.parseInt(args[i]); }
                case "--participation"-> { if (++i < args.length) rewardParticipation = Integer.parseInt(args[i]); }
                case "--winner"       -> { if (++i < args.length) winner = Integer.parseInt(args[i]); }
                case "--loser"        -> { if (++i < args.length) loser = Integer.parseInt(args[i]); }
            }
        }

        if (event.getType() == EventType.TEAM) {
            if (winner >= 0 && loser >= 0) {
                event.setRewardStrategy(new TeamReward(winner, loser));
            } else if (rewardMax >= 0 && rewardRange >= 0) {
                event.setRewardStrategy(new DegressiveReward(rewardMax, rewardRange, rewardParticipation));
            }
            event.setTeamConfig(new TeamConfig(teamCount, randomTeams, Map.of(), Map.of()));
        } else {
            if (rewardMax >= 0 && rewardRange >= 0) {
                event.setRewardStrategy(new DegressiveReward(rewardMax, rewardRange, rewardParticipation));
            }
        }
    }

    /** Pousse l'état mis à jour à tous les serveurs backend connectés. */
    public void pushStateToAll() {
        JsonObject update = new JsonObject();
        update.addProperty("type", MessageType.STATE_UPDATE.name());
        update.addProperty("playerId", "00000000-0000-0000-0000-000000000000");
        update.add("events", buildEventsArray());
        update.add("categories", buildCategoriesArray());
        update.add("games", buildGamesArray());

        byte[] data = gson.toJson(update).getBytes(StandardCharsets.UTF_8);
        proxy.getAllServers().forEach(rs ->
                rs.sendPluginMessage(CHANNEL, data));
    }

    // -------------------------------------------------------------------------

    private void replyError(ServerConnection server, UUID playerId, String msg) {
        JsonObject resp = new JsonObject();
        resp.addProperty("type", MessageType.ACTION_RESULT.name());
        resp.addProperty("playerId", playerId.toString());
        resp.addProperty("success", false);
        resp.addProperty("message", msg);
        send(server, resp);
    }

    private void send(ServerConnection server, JsonObject obj) {
        server.sendPluginMessage(CHANNEL, gson.toJson(obj).getBytes(StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------

    private JsonArray buildEventsArray() {
        JsonArray arr = new JsonArray();
        eventManager.getAll().forEach(e -> {
            JsonObject o = new JsonObject();
            o.addProperty("name", e.getName());
            o.addProperty("type", e.getType().name());
            o.addProperty("minParticipants", e.getMinParticipants());
            o.addProperty("maxParticipants", e.getMaxParticipants() == Integer.MAX_VALUE ? -1 : e.getMaxParticipants());
            o.addProperty("reward", e.getRewardStrategy().describe());
            o.addProperty("soloMode", e.getSoloMode() != null ? e.getSoloMode().name() : "");
            o.addProperty("category", e.getCategory() != null ? e.getCategory().getName() : "");
            o.addProperty("description", e.getDescription() != null ? e.getDescription() : "");
            o.addProperty("server", e.getServer() != null ? e.getServer() : "");
            arr.add(o);
        });
        return arr;
    }

    private JsonArray buildCategoriesArray() {
        JsonArray arr = new JsonArray();
        categoryManager.getAll().forEach(c -> {
            JsonObject o = new JsonObject();
            o.addProperty("name", c.getName());
            o.addProperty("description", c.getDescription() != null ? c.getDescription() : "");
            arr.add(o);
        });
        return arr;
    }

    private JsonArray buildGamesArray() {
        JsonArray arr = new JsonArray();
        gameManager.getActiveGames().forEach(g -> {
            JsonObject o = new JsonObject();
            o.addProperty("id", g.getId());
            o.addProperty("event", g.getEvent().getName());
            o.addProperty("status", g.getStatus().name());
            o.addProperty("participants", g.getParticipantCount());
            o.addProperty("maxParticipants", g.getEvent().getMaxParticipants() == Integer.MAX_VALUE ? -1 : g.getEvent().getMaxParticipants());
            arr.add(o);
        });
        return arr;
    }
}
