package fr.like.grackle.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.like.grackle.Grackle;
import fr.like.grackle.manager.CategoryManager;
import fr.like.grackle.manager.EventManager;
import fr.like.grackle.model.event.*;
import fr.like.grackle.model.reward.DegressiveReward;
import fr.like.grackle.model.reward.TeamReward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;

/**
 * /grackle-event create <nom> <solo|team> [options...]
 * /event list
 * /grackle-event info <nom>
 * /grackle-event delete <nom>
 *
 * Options pour solo  : --mode <ranked|participation> --max <n> --range <n> --participation <n>
 * Options pour team  : --teams <n> --random --max <n> --range <n> --participation <n>
 *                      --winner <n> --loser <n>  (TeamReward)
 * Options communes   : --min <n> --max-players <n> --category <nom> --desc <texte>
 */
public class EventCommand implements SimpleCommand {

    private final EventManager eventManager;
    private final CategoryManager categoryManager;
    private final Grackle plugin;

    public EventCommand(EventManager eventManager, CategoryManager categoryManager, Grackle plugin) {
        this.eventManager = eventManager;
        this.categoryManager = categoryManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation inv) {
        CommandSource src = inv.source();
        String[] args = inv.arguments();

        if (args.length == 0) {
            sendHelp(src);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(src, args);
            case "list"   -> handleList(src);
            case "info"   -> handleInfo(src, args);
            case "delete" -> handleDelete(src, args);
            default       -> src.sendMessage(Component.text("Sous-commande inconnue.", NamedTextColor.RED));
        }
    }

    // -------------------------------------------------------------------------

    private void handleCreate(CommandSource src, String[] args) {
        if (args.length < 3) {
            src.sendMessage(Component.text("Usage: /grackle-event create <nom> <solo|team> [options]", NamedTextColor.RED));
            return;
        }

        String name = args[1];
        if (eventManager.get(name).isPresent()) {
            src.sendMessage(Component.text("Un event nommé '" + name + "' existe déjà.", NamedTextColor.RED));
            return;
        }

        EventType type;
        try {
            type = EventType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            src.sendMessage(Component.text("Type invalide. Utilisez 'solo' ou 'team'.", NamedTextColor.RED));
            return;
        }

        Event event = new Event(name, type);
        parseOptions(event, args, 3);

        // Valeurs par défaut
        if (event.getRewardStrategy() == null) {
            if (type == EventType.TEAM) {
                event.setRewardStrategy(new TeamReward(3, 1));
            } else {
                event.setRewardStrategy(new DegressiveReward(5, 1, 1));
            }
        }
        if (type == EventType.SOLO && event.getSoloMode() == null) {
            event.setSoloMode(SoloMode.RANKED);
        }
        if (type == EventType.TEAM && event.getTeamConfig() == null) {
            event.setTeamConfig(new TeamConfig(2, false, Map.of(), Map.of()));
        }

        eventManager.register(event);
        plugin.save();
        src.sendMessage(Component.text("Event '" + name + "' (" + type + ") créé. Récompense : "
                + event.getRewardStrategy().describe(), NamedTextColor.GREEN));
    }

    private void parseOptions(Event event, String[] args, int start) {
        int teamCount = 2;
        boolean randomTeams = false;
        int rewardMax = -1, rewardRange = -1, rewardParticipation = 1;
        int winner = -1, loser = -1;

        for (int i = start; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "--mode"         -> { if (++i < args.length) event.setSoloMode(parseSoloMode(args[i])); }
                case "--min"          -> { if (++i < args.length) event.setMinParticipants(parseInt(args[i], 2)); }
                case "--max-players"  -> { if (++i < args.length) event.setMaxParticipants(parseInt(args[i], Integer.MAX_VALUE)); }
                case "--category"     -> { if (++i < args.length) categoryManager.get(args[i]).ifPresent(event::setCategory); }
                case "--desc"         -> { if (++i < args.length) event.setDescription(args[i]); }
                case "--teams"        -> { if (++i < args.length) teamCount = parseInt(args[i], 2); }
                case "--random"       -> randomTeams = true;
                case "--max"          -> { if (++i < args.length) rewardMax = parseInt(args[i], 5); }
                case "--range"        -> { if (++i < args.length) rewardRange = parseInt(args[i], 1); }
                case "--participation"-> { if (++i < args.length) rewardParticipation = parseInt(args[i], 1); }
                case "--winner"       -> { if (++i < args.length) winner = parseInt(args[i], 3); }
                case "--loser"        -> { if (++i < args.length) loser = parseInt(args[i], 1); }
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

    private void handleList(CommandSource src) {
        if (eventManager.getAll().isEmpty()) {
            src.sendMessage(Component.text("Aucun event enregistré.", NamedTextColor.GRAY));
            return;
        }
        eventManager.getAll().forEach(e ->
                src.sendMessage(Component.text(
                        "- " + e.getName() + " [" + e.getType() + "] " + e.getRewardStrategy().describe(),
                        NamedTextColor.AQUA)));
    }

    private void handleInfo(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(Component.text("Usage: /grackle-event info <nom>", NamedTextColor.RED)); return; }
        eventManager.get(args[1]).ifPresentOrElse(e -> {
            src.sendMessage(Component.text("=== " + e.getName() + " ===", NamedTextColor.GOLD));
            src.sendMessage(Component.text("Type : " + e.getType(), NamedTextColor.WHITE));
            src.sendMessage(Component.text("Joueurs : " + e.getMinParticipants() + "-" + (e.getMaxParticipants() == Integer.MAX_VALUE ? "∞" : e.getMaxParticipants()), NamedTextColor.WHITE));
            src.sendMessage(Component.text("Récompense : " + e.getRewardStrategy().describe(), NamedTextColor.WHITE));
            if (e.getCategory() != null)
                src.sendMessage(Component.text("Catégorie : " + e.getCategory().getName(), NamedTextColor.WHITE));
            if (e.getDescription() != null)
                src.sendMessage(Component.text("Description : " + e.getDescription(), NamedTextColor.GRAY));
        }, () -> src.sendMessage(Component.text("Event introuvable.", NamedTextColor.RED)));
    }

    private void handleDelete(CommandSource src, String[] args) {
        if (args.length < 2) { src.sendMessage(Component.text("Usage: /grackle-event delete <nom>", NamedTextColor.RED)); return; }
        if (eventManager.remove(args[1])) {
            plugin.save();
            src.sendMessage(Component.text("Event '" + args[1] + "' supprimé.", NamedTextColor.GREEN));
        } else {
            src.sendMessage(Component.text("Event introuvable.", NamedTextColor.RED));
        }
    }

    // -------------------------------------------------------------------------

    private SoloMode parseSoloMode(String s) {
        try { return SoloMode.valueOf(s.toUpperCase()); } catch (IllegalArgumentException e) { return SoloMode.RANKED; }
    }

    private int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private void sendHelp(CommandSource src) {
        src.sendMessage(Component.text("/grackle-event create <nom> <solo|team> [options]", NamedTextColor.YELLOW));
        src.sendMessage(Component.text("/grackle-event list | info <nom> | delete <nom>", NamedTextColor.YELLOW));
        src.sendMessage(Component.text("Options: --min --max-players --category --desc", NamedTextColor.GRAY));
        src.sendMessage(Component.text("Solo: --mode <ranked|participation> --max --range --participation", NamedTextColor.GRAY));
        src.sendMessage(Component.text("Team: --teams --random --winner --loser | --max --range --participation", NamedTextColor.GRAY));
    }

    @Override
    public List<String> suggest(Invocation inv) {
        String[] args = inv.arguments();
        if (args.length <= 1) return List.of("create", "list", "info", "delete");
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete")))
            return eventManager.getAll().stream().map(Event::getName).toList();
        if (args.length == 3 && args[0].equalsIgnoreCase("create"))
            return List.of("solo", "team");
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        return inv.source().hasPermission("grackle.event");
    }
}
