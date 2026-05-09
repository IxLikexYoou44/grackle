package fr.like.grackle.paper.messaging;

import com.google.gson.*;
import fr.like.grackle.paper.GrackleUI;
import fr.like.grackle.paper.menu.GameListMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class PaperMessagingHandler implements PluginMessageListener {

    public static final String CHANNEL = "grackle:main";

    private final GrackleUI plugin;
    private final Gson gson = new GsonBuilder().create();

    public PaperMessagingHandler(GrackleUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player,
                                        byte @NotNull [] message) {
        if (!channel.equals(CHANNEL)) return;

        String raw = new String(message, StandardCharsets.UTF_8);
        JsonObject msg;
        try {
            msg = gson.fromJson(raw, JsonObject.class);
        } catch (JsonParseException e) {
            plugin.getLogger().warning("Message plugin malformé : " + raw);
            return;
        }

        String type = msg.get("type").getAsString();

        switch (type) {
            case "STATE_RESPONSE", "STATE_UPDATE" -> {
                plugin.getDataCache().updateFromPayload(msg);
            }
            case "ACTION_RESULT" -> {
                boolean success = msg.get("success").getAsBoolean();
                String text = msg.get("message").getAsString();

                if (msg.has("games")) {
                    plugin.getDataCache().updateFromPayload(msg);
                }

                // Notifie le joueur concerné
                String playerId = msg.get("playerId").getAsString();
                plugin.getServer().getOnlinePlayers().stream()
                        .filter(p -> p.getUniqueId().toString().equals(playerId))
                        .findFirst()
                        .ifPresent(p -> {
                            NamedTextColor color = success ? NamedTextColor.GREEN : NamedTextColor.RED;
                            p.sendMessage(Component.text("[Grackle] " + text, color));

                            // Rafraîchit le menu ouvert si c'est un GameListMenu
                            if (success && p.getOpenInventory().getTopInventory().getHolder() instanceof GameListMenu) {
                                plugin.getMenuManager().openGameList(p);
                            }
                        });
            }
        }
    }

    /** Envoie une action à Velocity. */
    public void sendAction(Player player, String action, Consumer<JsonObject> params) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "ACTION");
        msg.addProperty("playerId", player.getUniqueId().toString());
        msg.addProperty("action", action);
        JsonObject p = new JsonObject();
        params.accept(p);
        msg.add("params", p);
        player.sendPluginMessage(plugin, CHANNEL, gson.toJson(msg).getBytes(StandardCharsets.UTF_8));
    }

    /** Demande l'état complet à Velocity (appelé à la connexion du joueur). */
    public void requestState(Player player) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "REQUEST_STATE");
        msg.addProperty("playerId", player.getUniqueId().toString());
        player.sendPluginMessage(plugin, CHANNEL, gson.toJson(msg).getBytes(StandardCharsets.UTF_8));
    }
}
