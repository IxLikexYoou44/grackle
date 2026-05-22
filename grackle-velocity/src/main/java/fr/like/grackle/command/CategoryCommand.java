package fr.like.grackle.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import fr.like.grackle.Grackle;
import fr.like.grackle.manager.CategoryManager;
import fr.like.grackle.model.event.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

/**
 * /grackle-category create <nom>
 * /grackle-category list
 * /grackle-category delete <nom>
 */
public class CategoryCommand implements SimpleCommand {

    private final CategoryManager categoryManager;
    private final Grackle plugin;

    public CategoryCommand(CategoryManager categoryManager, Grackle plugin) {
        this.categoryManager = categoryManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation inv) {
        CommandSource src = inv.source();
        String[] args = inv.arguments();

        if (args.length == 0) {
            src.sendMessage(Component.text("Usage: /grackle-category <create|list|delete> [nom]", NamedTextColor.YELLOW));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 2) { src.sendMessage(Component.text("Usage: /grackle-category create <nom>", NamedTextColor.RED)); return; }
                String name = args[1];
                if (categoryManager.exists(name)) {
                    src.sendMessage(Component.text("La catégorie '" + name + "' existe déjà.", NamedTextColor.RED));
                    return;
                }
                Category cat = categoryManager.create(name);
                if (args.length >= 3) cat.setDescription(String.join(" ", List.of(args).subList(2, args.length)));
                plugin.save();
                src.sendMessage(Component.text("Catégorie '" + name + "' créée.", NamedTextColor.GREEN));
            }
            case "list" -> {
                if (categoryManager.getAll().isEmpty()) {
                    src.sendMessage(Component.text("Aucune catégorie.", NamedTextColor.GRAY));
                    return;
                }
                categoryManager.getAll().forEach(c ->
                        src.sendMessage(Component.text("- " + c.getName(), NamedTextColor.AQUA)));
            }
            case "delete" -> {
                if (args.length < 2) { src.sendMessage(Component.text("Usage: /grackle-category delete <nom>", NamedTextColor.RED)); return; }
                if (categoryManager.remove(args[1])) {
                    plugin.save();
                    src.sendMessage(Component.text("Catégorie '" + args[1] + "' supprimée.", NamedTextColor.GREEN));
                } else {
                    src.sendMessage(Component.text("Catégorie introuvable.", NamedTextColor.RED));
                }
            }
            default -> src.sendMessage(Component.text("Sous-commande inconnue.", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> suggest(Invocation inv) {
        String[] args = inv.arguments();
        if (args.length <= 1) return List.of("create", "list", "delete");
        if (args.length == 2 && args[0].equalsIgnoreCase("delete"))
            return categoryManager.getAll().stream().map(Category::getName).toList();
        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation inv) {
        return inv.source().hasPermission("grackle.category");
    }
}
