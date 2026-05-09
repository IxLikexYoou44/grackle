package fr.like.grackle.model.event;

public class Category {

    private final String name;
    private String description;

    public Category(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
