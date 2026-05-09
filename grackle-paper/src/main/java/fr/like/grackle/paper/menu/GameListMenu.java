package fr.like.grackle.paper.menu;

import com.google.gson.JsonObject;
import fr.like.grackle.paper.GrackleUI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class GameListMenu extends AbstractMenu {

    private final GrackleUI plugin;
    private final List<JsonObject> games;

    public GameListMenu(GrackleUI plugin) {
        this.plugin = plugin;
        this.games = new ArrayList<>(plugin.getDataCache().getGames());
    }

    @Override protected String title() { return "⚔ Parties en cours"; }
    @Override protected int size()    { return 54; }

    @Override
    protected void populate() {
        for (int i = 0; i < size(); i++) inventory.setItem(i, ItemBuilder.filler());

        if (games.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER)
                    .name("Aucune partie active", NamedTextColor.RED)
                    .lore("Lance un événement pour", "créer une partie.")
                    .build());
        }

        for (int i = 0; i < Math.min(games.size(), 45); i++) {
            JsonObject g = games.get(i);
            int id = g.get("id").getAsInt();
            String event = g.get("event").getAsString();
            String status = g.get("status").getAsString();
            int participants = g.get("participants").getAsInt();
            int max = g.get("maxParticipants").getAsInt();
            String maxStr = max == -1 ? "∞" : String.valueOf(max);

            Material mat = switch (status) {
                case "WAITING" -> Material.LIME_WOOL;
                case "IN_GAME" -> Material.ORANGE_WOOL;
                default        -> Material.RED_WOOL;
            };

            inventory.setItem(i, new ItemBuilder(mat)
                    .name("Partie #" + id + " — " + event, NamedTextColor.WHITE)
                    .lore(
                            "Statut : " + status,
                            "Joueurs : " + participants + "/" + maxStr,
                            "",
                            "▶ Clic gauche : Rejoindre",
                            "▶ Clic droit : Quitter",
                            "▶ Shift+clic : Terminer (admin)"
                    )
                    .build());
        }

        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name("Retour", NamedTextColor.RED)
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 49) { plugin.getMenuManager().openMain(player); return; }
        if (slot < 0 || slot >= games.size()) return;

        JsonObject g = games.get(slot);
        int gameId = g.get("id").getAsInt();

        if (event.isLeftClick() && !event.isShiftClick()) {
            plugin.getMessagingHandler().sendAction(player, "JOIN_GAME",
                    obj -> obj.addProperty("gameId", gameId));
        } else if (event.isRightClick() && !event.isShiftClick()) {
            plugin.getMessagingHandler().sendAction(player, "LEAVE_GAME",
                    obj -> obj.addProperty("gameId", gameId));
        } else if (event.isShiftClick()) {
            plugin.getMessagingHandler().sendAction(player, "END_GAME",
                    obj -> obj.addProperty("gameId", gameId));
        }
    }
}
