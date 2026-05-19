package fr.like.grackle.paper.menu;

import com.google.gson.JsonObject;
import fr.like.grackle.paper.GrackleUI;
import fr.like.grackle.paper.data.DataCache;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class EventListMenu extends AbstractMenu {

    private final GrackleUI plugin;
    private final List<JsonObject> events;

    public EventListMenu(GrackleUI plugin) {
        this.plugin = plugin;
        this.events = new ArrayList<>(plugin.getDataCache().getEvents());
    }

    @Override protected String title() { return "📋 Événements"; }
    @Override protected int size()    { return 54; }

    @Override
    protected void populate() {
        for (int i = 0; i < size(); i++) inventory.setItem(i, ItemBuilder.filler());

        for (int i = 0; i < Math.min(events.size(), 45); i++) {
            JsonObject ev = events.get(i);
            String name     = ev.get("name").getAsString();
            String type     = ev.get("type").getAsString();
            String reward   = ev.get("reward").getAsString();
            String category = ev.get("category").getAsString();
            String desc     = ev.get("description").getAsString();
            int min = ev.get("minParticipants").getAsInt();
            int max = ev.get("maxParticipants").getAsInt();
            String maxStr = max == -1 ? "∞" : String.valueOf(max);

            Material mat = type.equals("TEAM") ? Material.SHIELD : Material.DIAMOND;
            inventory.setItem(i, new ItemBuilder(mat)
                    .name(name, NamedTextColor.YELLOW)
                    .lore(
                            "Type : " + type,
                            "Joueurs : " + min + "-" + maxStr,
                            "Récompense : " + reward,
                            category.isEmpty() ? "" : "Catégorie : " + category,
                            desc.isEmpty() ? "" : desc,
                            "",
                            "▶ Clic gauche : Lancer une partie",
                            "▶ Clic droit : Détails"
                    )
                    .build());
        }

        // Bouton retour
        inventory.setItem(49, new ItemBuilder(Material.ARROW)
                .name("Retour", NamedTextColor.RED)
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 49) {
            plugin.getMenuManager().openMain(player);
            return;
        }

        if (slot >= 0 && slot < events.size()) {
            JsonObject ev = events.get(slot);
            String eventName = ev.get("name").getAsString();

            if (event.isLeftClick()) {
                plugin.getMessagingHandler().sendAction(player, "START_GAME", obj -> {
                    obj.addProperty("event", eventName);
                    if (ev.has("server") && !ev.get("server").getAsString().isEmpty()) obj.addProperty("targetServer", ev.get("server").getAsString());
                });
            } else if (event.isRightClick()) {
                plugin.getMenuManager().openEventDetail(player, ev);
            }
        }
    }
}
