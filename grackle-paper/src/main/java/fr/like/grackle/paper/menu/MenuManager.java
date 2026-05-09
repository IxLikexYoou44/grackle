package fr.like.grackle.paper.menu;

import com.google.gson.JsonObject;
import fr.like.grackle.paper.GrackleUI;
import org.bukkit.entity.Player;

public class MenuManager {

    private final GrackleUI plugin;

    public MenuManager(GrackleUI plugin) {
        this.plugin = plugin;
    }

    public void openMain(Player player) {
        new MainMenu(plugin).open(player);
    }

    public void openEventList(Player player) {
        new EventListMenu(plugin).open(player);
    }

    public void openEventDetail(Player player, JsonObject event) {
        new EventDetailMenu(plugin, event).open(player);
    }

    public void openGameList(Player player) {
        new GameListMenu(plugin).open(player);
    }

    public void openCategoryList(Player player) {
        new CategoryListMenu(plugin).open(player);
    }
}
