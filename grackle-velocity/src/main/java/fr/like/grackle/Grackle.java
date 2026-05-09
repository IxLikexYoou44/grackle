package fr.like.grackle;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import fr.like.grackle.command.CategoryCommand;
import fr.like.grackle.command.EventCommand;
import fr.like.grackle.command.GrackleCommand;
import fr.like.grackle.manager.CategoryManager;
import fr.like.grackle.manager.EventManager;
import fr.like.grackle.manager.GameManager;
import fr.like.grackle.messaging.VelocityMessagingHandler;
import fr.like.grackle.storage.StorageManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

@Plugin(
        id = "grackle",
        name = "Grackle",
        version = "1.0",
        description = "Gestion d'événements et de parties multijoueurs"
)
public class Grackle {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private CategoryManager categoryManager;
    private EventManager eventManager;
    private GameManager gameManager;
    private StorageManager storageManager;
    private VelocityMessagingHandler messagingHandler;

    @Inject
    public Grackle(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        categoryManager  = new CategoryManager();
        eventManager     = new EventManager();
        gameManager      = new GameManager(logger);
        storageManager   = new StorageManager(dataDirectory, logger);

        try {
            storageManager.init();
            storageManager.loadCategories(categoryManager);
            storageManager.loadEvents(eventManager, categoryManager);
        } catch (IOException e) {
            logger.error("Erreur lors du chargement des données : {}", e.getMessage());
        }

        messagingHandler = new VelocityMessagingHandler(proxy, logger, this,
                eventManager, categoryManager, gameManager);
        proxy.getChannelRegistrar().register(VelocityMessagingHandler.CHANNEL);
        proxy.getEventManager().register(this, messagingHandler);

        registerCommands();
        logger.info("Grackle initialisé.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        storageManager.saveCategories(categoryManager);
        storageManager.saveEvents(eventManager);
        logger.info("Données sauvegardées.");
    }

    public void save() {
        storageManager.saveCategories(categoryManager);
        storageManager.saveEvents(eventManager);
    }

    private void registerCommands() {
        CommandManager cm = proxy.getCommandManager();

        CommandMeta grackle = cm.metaBuilder("grackle").aliases("gk").plugin(this).build();
        cm.register(grackle, new GrackleCommand(proxy, eventManager, gameManager));

        CommandMeta eventCmd = cm.metaBuilder("event").aliases("ev").plugin(this).build();
        cm.register(eventCmd, new EventCommand(eventManager, categoryManager, this));

        CommandMeta categoryCmd = cm.metaBuilder("category").aliases("cat").plugin(this).build();
        cm.register(categoryCmd, new CategoryCommand(categoryManager, this));
    }
}
