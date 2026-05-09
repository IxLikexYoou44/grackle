package fr.like.grackle.manager;

import fr.like.grackle.model.event.Category;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CategoryManager {

    private final Map<String, Category> categories = new HashMap<>();

    public Category create(String name) {
        Category c = new Category(name);
        categories.put(name.toLowerCase(), c);
        return c;
    }

    public boolean exists(String name) {
        return categories.containsKey(name.toLowerCase());
    }

    public Optional<Category> get(String name) {
        return Optional.ofNullable(categories.get(name.toLowerCase()));
    }

    public boolean remove(String name) {
        return categories.remove(name.toLowerCase()) != null;
    }

    public Collection<Category> getAll() {
        return categories.values();
    }
}
