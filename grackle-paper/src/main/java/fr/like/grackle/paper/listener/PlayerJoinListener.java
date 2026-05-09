package fr.like.grackle.paper.listener;

import fr.like.grackle.paper.GrackleUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final GrackleUI plugin;

    public PlayerJoinListener(GrackleUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Demande l'état à Velocity dès qu'un joueur se connecte
        // Petit délai pour laisser la connexion plugin messaging s'établir
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getMessagingHandler().requestState(event.getPlayer()), 20L);
    }
}
