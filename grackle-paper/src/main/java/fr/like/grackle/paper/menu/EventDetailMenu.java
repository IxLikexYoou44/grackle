package fr.like.grackle.paper.menu;

import com.google.gson.JsonObject;
import fr.like.grackle.paper.GrackleUI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EventDetailMenu extends AbstractMenu {

    private final GrackleUI plugin;
    private final JsonObject event;

    public EventDetailMenu(GrackleUI plugin, JsonObject event) {
        this.plugin = plugin;
        this.event = event;
    }

    @Override protected String title() { return "📄 " + event.get("name").getAsString(); }
    @Override protected int size()    { return 27; }

    @Override
    protected void populate() {
        for (int i = 0; i < size(); i++) inventory.setItem(i, ItemBuilder.filler());

        String name     = event.get("name").getAsString();
        String type     = event.get("type").getAsString();
        String reward   = event.get("reward").getAsString();
        String category = event.get("category").getAsString();
        String desc     = event.get("description").getAsString();
        int min = event.get("minParticipants").getAsInt();
        int max = event.get("maxParticipants").getAsInt();
        String maxStr = max == -1 ? "∞" : String.valueOf(max);

        // Info
        inventory.setItem(13, new ItemBuilder(Material.PAPER)
                .name(name, NamedTextColor.YELLOW)
                .lore(
                        "Type : " + type,
                        "Joueurs : " + min + " - " + maxStr,
                        "Récompense : " + reward,
                        category.isEmpty() ? "" : "Catégorie : " + category,
                        desc.isEmpty() ? "" : " " + desc
                )
                .build());

        // Lancer
        inventory.setItem(11, new ItemBuilder(Material.LIME_WOOL)
                .name("Lancer une partie", NamedTextColor.GREEN)
                .lore("Créer une partie pour", "cet événement.")
                .build());

        // Retour
        inventory.setItem(15, new ItemBuilder(Material.ARROW)
                .name("Retour", NamedTextColor.RED)
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        switch (e.getSlot()) {
            case 11 -> {
                String eventName = event.get("name").getAsString();
                plugin.getMessagingHandler().sendAction(player, "START_GAME", obj -> {
                    obj.addProperty("event", eventName);
                    if (event.has("server") && !event.get("server").getAsString().isEmpty()) {
                        obj.addProperty("targetServer", event.get("server").getAsString());
                    }
                });
            }
            case 15 -> plugin.getMenuManager().openEventList(player);
        }
    }
}
