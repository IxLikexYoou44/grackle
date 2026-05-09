package fr.like.grackle.paper.menu;

import fr.like.grackle.paper.GrackleUI;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MainMenu extends AbstractMenu {

    private final GrackleUI plugin;

    public MainMenu(GrackleUI plugin) {
        this.plugin = plugin;
    }

    @Override protected String title() { return "⚙ Grackle"; }
    @Override protected int size()    { return 27; }

    @Override
    protected void populate() {
        for (int i = 0; i < size(); i++) inventory.setItem(i, ItemBuilder.filler());

        inventory.setItem(11, new ItemBuilder(Material.BOOK)
                .name("Événements", NamedTextColor.AQUA)
                .lore("Créer, gérer et consulter", "les événements disponibles.")
                .build());

        inventory.setItem(13, new ItemBuilder(Material.IRON_SWORD)
                .name("Parties en cours", NamedTextColor.GOLD)
                .lore("Rejoindre ou gérer", "les parties actives.")
                .build());

        inventory.setItem(15, new ItemBuilder(Material.BOOKSHELF)
                .name("Catégories", NamedTextColor.GREEN)
                .lore("Gérer les catégories", "d'événements.")
                .build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        switch (event.getSlot()) {
            case 11 -> plugin.getMenuManager().openEventList(player);
            case 13 -> plugin.getMenuManager().openGameList(player);
            case 15 -> plugin.getMenuManager().openCategoryList(player);
        }
    }
}
