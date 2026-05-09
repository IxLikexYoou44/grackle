package fr.like.grackle.paper.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMenu implements InventoryHolder {

    protected Inventory inventory;

    protected abstract String title();
    protected abstract int size();
    protected abstract void populate();

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, size(), net.kyori.adventure.text.Component.text(title()));
        populate();
        player.openInventory(inventory);
    }

    public abstract void onClick(InventoryClickEvent event);

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
