package fr.like.grackle.paper.menu;

import com.google.gson.JsonObject;
import fr.like.grackle.paper.GrackleUI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class CategoryListMenu extends AbstractMenu {

    private final GrackleUI plugin;
    private final List<JsonObject> categories;

    public CategoryListMenu(GrackleUI plugin) {
        this.plugin = plugin;
        this.categories = new ArrayList<>(plugin.getDataCache().getCategories());
    }

    @Override protected String title() { return "🗂 Catégories"; }
    @Override protected int size()    { return 27; }

    @Override
    protected void populate() {
        for (int i = 0; i < size(); i++) inventory.setItem(i, ItemBuilder.filler());

        for (int i = 0; i < Math.min(categories.size(), 18); i++) {
            JsonObject cat = categories.get(i);
            String name = cat.get("name").getAsString();
            String desc = cat.get("description").getAsString();

            inventory.setItem(i, new ItemBuilder(Material.BOOKSHELF)
                    .name(name, NamedTextColor.GREEN)
                    .lore(desc.isEmpty() ? "Aucune description." : desc)
                    .build());
        }

        inventory.setItem(22, new ItemBuilder(Material.ARROW)
                .name("Retour", NamedTextColor.RED)
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        if (event.getSlot() == 22) plugin.getMenuManager().openMain(player);
    }
}
