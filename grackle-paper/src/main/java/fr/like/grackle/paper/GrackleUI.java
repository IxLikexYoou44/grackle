package fr.like.grackle.paper;

import fr.like.grackle.paper.data.DataCache;
import fr.like.grackle.paper.listener.MenuListener;
import fr.like.grackle.paper.listener.PlayerJoinListener;
import fr.like.grackle.paper.menu.MenuManager;
import fr.like.grackle.paper.messaging.PaperMessagingHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class GrackleUI extends JavaPlugin {

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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("grackle") || command.getName().equalsIgnoreCase("gk")) {
            messagingHandler.requestState(player);
            // Ouvre le menu après une courte attente pour recevoir les données
            getServer().getScheduler().runTaskLater(this, () -> menuManager.openMain(player), 2L);
        }
        return true;
    }

    public DataCache getDataCache()                    { return dataCache; }
    public MenuManager getMenuManager()                { return menuManager; }
    public PaperMessagingHandler getMessagingHandler() { return messagingHandler; }
}
