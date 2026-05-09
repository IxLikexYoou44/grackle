package fr.like.grackle.paper.listener;

import fr.like.grackle.paper.GrackleUI;
import fr.like.grackle.paper.menu.AbstractMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class MenuListener implements Listener {

    private final GrackleUI plugin;

    public MenuListener(GrackleUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof AbstractMenu menu)) return;

        // Ignore les clics dans l'inventaire du joueur (bas)
        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            event.setCancelled(true);
            return;
        }

        menu.onClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Rien à faire pour l'instant, extensible si nécessaire
    }
}
