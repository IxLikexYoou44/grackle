package fr.like.grackle.paper;

import fr.like.grackle.paper.data.DataCache;
import fr.like.grackle.paper.listener.MenuListener;
import fr.like.grackle.paper.listener.PlayerJoinListener;
import fr.like.grackle.paper.menu.MenuManager;
import fr.like.grackle.paper.messaging.PaperMessagingHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class GrackleUI extends JavaPlugin implements CommandExecutor, TabCompleter {

    private DataCache dataCache;
    private MenuManager menuManager;
    private PaperMessagingHandler messagingHandler;

    @Override
    public void onEnable() {
        dataCache        = new DataCache();
        menuManager      = new MenuManager(this);
        messagingHandler = new PaperMessagingHandler(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, PaperMessagingHandler.CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, PaperMessagingHandler.CHANNEL, messagingHandler);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Register command executor and tab completer to ensure onCommand/onTabComplete are used
        if (getCommand("grackle") != null) getCommand("grackle").setExecutor(this);
        if (getCommand("grackle-event") != null) getCommand("grackle-event").setExecutor(this);
        if (getCommand("grackle-category") != null) getCommand("grackle-category").setExecutor(this);
        if (getCommand("grackle-event") != null) getCommand("grackle-event").setTabCompleter(this);
        if (getCommand("grackle-category") != null) getCommand("grackle-category").setTabCompleter(this);

        getLogger().info("GrackleUI activé.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        getLogger().info("GrackleUI.onCommand invoked by {} for command '{}' with args={}", sender.getName(), cmd, java.util.Arrays.toString(args));

        // /grackle - opens UI (existing behavior)
        if (cmd.equals("grackle") || cmd.equals("gk")) {
            if (!(sender instanceof Player player)) { sender.sendMessage("Cette commande est réservée aux joueurs."); return true; }
            messagingHandler.requestState(player);
            getServer().getScheduler().runTaskLater(this, () -> menuManager.openMain(player), 2L);
            return true;
        }

        // Helper to send plugin message from non-player (console)
        java.util.function.BiConsumer<String, java.util.function.Consumer<com.google.gson.JsonObject>> sendFrom = (action, consumer) -> {
            com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
            msg.addProperty("type", "ACTION");
            msg.addProperty("playerId", "00000000-0000-0000-0000-000000000000");
            msg.addProperty("action", action);
            com.google.gson.JsonObject p = new com.google.gson.JsonObject();
            if (consumer != null) consumer.accept(p);
            msg.add("params", p);
            String json = new com.google.gson.Gson().toJson(msg);
            getLogger().info("Sending plugin message to proxy: {}", json);
            getServer().sendPluginMessage(this, PaperMessagingHandler.CHANNEL, json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            sender.sendMessage("Requête envoyée au proxy.");
        };

        // /category forwarded to Velocity
        if (cmd.equals("category") || cmd.equals("cat") || cmd.equals("grackle-category") || cmd.equals("gkcategory")) {
            if (args.length == 0) { sender.sendMessage("Usage: /category <create|list|delete> [nom] [desc]"); return true; }
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "create" -> {
                    if (args.length < 2) { sender.sendMessage("Usage: /category create <nom> [description]"); return true; }
                    String name = args[1];
                    String desc = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
                    getLogger().info("Forwarding CATEGORY_CREATE name='{}' desc='{}' from {}", name, desc, sender.getName());
                    if (sender instanceof Player player) {
                        messagingHandler.sendAction(player, "CATEGORY_CREATE", obj -> { obj.addProperty("name", name); obj.addProperty("description", desc); });
                    } else {
                        sendFrom.accept("CATEGORY_CREATE", obj -> { obj.addProperty("name", name); obj.addProperty("description", desc); });
                    }
                }
                case "delete" -> {
                    if (args.length < 2) { sender.sendMessage("Usage: /category delete <nom>"); return true; }
                    String name = args[1];
                    getLogger().info("Forwarding CATEGORY_DELETE name='{}' from {}", name, sender.getName());
                    if (sender instanceof Player player) {
                        messagingHandler.sendAction(player, "CATEGORY_DELETE", obj -> obj.addProperty("name", name));
                    } else {
                        sendFrom.accept("CATEGORY_DELETE", obj -> obj.addProperty("name", name));
                    }
                }
                case "list" -> {
                    if (dataCache.getCategories().isEmpty()) { sender.sendMessage("Aucune catégorie."); return true; }
                    dataCache.getCategories().forEach(c -> sender.sendMessage("- " + c.get("name").getAsString()));
                }
                default -> sender.sendMessage("Sous-commande inconnue. Usage: /category <create|list|delete>");
            }
            return true;
        }

        // /event forwarded to Velocity
        if (cmd.equals("event") || cmd.equals("ev") || cmd.equals("grackle-event") || cmd.equals("gkevent")) {
            if (args.length == 0) { sender.sendMessage("Usage: /event <create|list|info|delete> ..."); return true; }
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "create" -> {
                    if (args.length < 3) { sender.sendMessage("Usage: /event create <nom> <solo|team> [options]"); return true; }
                    String name = args[1];
                    String type = args[2].toUpperCase();
                    String[] options = args.length > 3 ? java.util.Arrays.copyOfRange(args, 3, args.length) : new String[0];
                    getLogger().info("Forwarding EVENT_CREATE name='{}' type='{}' options={} from {}", name, type, java.util.Arrays.toString(options), sender.getName());
                    if (sender instanceof Player player) {
                        messagingHandler.sendAction(player, "EVENT_CREATE", obj -> {
                            obj.addProperty("name", name);
                            obj.addProperty("type", type);
                            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                            for (String o : options) arr.add(o);
                            obj.add("options", arr);
                        });
                    } else {
                        sendFrom.accept("EVENT_CREATE", obj -> {
                            obj.addProperty("name", name);
                            obj.addProperty("type", type);
                            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                            for (String o : options) arr.add(o);
                            obj.add("options", arr);
                        });
                    }
                }
                case "delete" -> {
                    if (args.length < 2) { sender.sendMessage("Usage: /event delete <nom>"); return true; }
                    String name = args[1];
                    getLogger().info("Forwarding EVENT_DELETE name='{}' from {}", name, sender.getName());
                    if (sender instanceof Player player) {
                        messagingHandler.sendAction(player, "EVENT_DELETE", obj -> obj.addProperty("name", name));
                    } else {
                        sendFrom.accept("EVENT_DELETE", obj -> obj.addProperty("name", name));
                    }
                }
                case "list" -> {
                    if (dataCache.getEvents().isEmpty()) { sender.sendMessage("Aucun event."); return true; }
                    dataCache.getEvents().forEach(e -> sender.sendMessage("- " + e.get("name").getAsString() + " [" + e.get("type").getAsString() + "]"));
                }
                case "info" -> {
                    if (args.length < 2) { sender.sendMessage("Usage: /event info <nom>"); return true; }
                    String name = args[1];
                    java.util.Optional<com.google.gson.JsonObject> found = dataCache.getEvents().stream()
                            .filter(o -> o.get("name").getAsString().equalsIgnoreCase(name)).findFirst();
                    if (found.isEmpty()) { sender.sendMessage("Event introuvable."); return true; }
                    com.google.gson.JsonObject e = found.get();
                    sender.sendMessage("=== " + e.get("name").getAsString() + " ===");
                    sender.sendMessage("Type : " + e.get("type").getAsString());
                    sender.sendMessage("Joueurs : " + e.get("minParticipants").getAsInt() + " - " + (e.get("maxParticipants").getAsInt() == -1 ? "∞" : e.get("maxParticipants").getAsInt()));
                    sender.sendMessage("Récompense : " + e.get("reward").getAsString());
                    if (!e.get("category").getAsString().isEmpty()) sender.sendMessage("Catégorie : " + e.get("category").getAsString());
                    if (!e.get("description").getAsString().isEmpty()) sender.sendMessage("Description : " + e.get("description").getAsString());
                }
                default -> sender.sendMessage("Sous-commande inconnue. Usage: /event <create|list|info|delete>");
            }
            return true;
        }

        return false;
    }

    @Override
    public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmd = command.getName().toLowerCase();
        if (cmd.equals("category") || cmd.equals("cat") || cmd.equals("grackle-category") || cmd.equals("gkcategory")) {
            if (args.length == 1) return java.util.List.of("create", "list", "delete");
            if (args.length == 2 && args[0].equalsIgnoreCase("delete")) return dataCache.getCategories().stream().map(o -> o.get("name").getAsString()).toList();
            return java.util.List.of();
        }
        if (cmd.equals("event") || cmd.equals("ev") || cmd.equals("grackle-event") || cmd.equals("gkevent")) {
            if (args.length == 1) return java.util.List.of("create", "list", "info", "delete");
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("create")) return java.util.List.of("solo", "team");
                if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete")) return dataCache.getEvents().stream().map(o -> o.get("name").getAsString()).toList();
            }
            return java.util.List.of();
        }
        return java.util.List.of();
    }

    public DataCache getDataCache()                    { return dataCache; }
    public MenuManager getMenuManager()                { return menuManager; }
    public PaperMessagingHandler getMessagingHandler() { return messagingHandler; }
}
