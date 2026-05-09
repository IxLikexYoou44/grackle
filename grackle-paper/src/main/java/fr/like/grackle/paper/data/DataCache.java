package fr.like.grackle.paper.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataCache {

    private final List<JsonObject> events     = new ArrayList<>();
    private final List<JsonObject> categories = new ArrayList<>();
    private final List<JsonObject> games      = new ArrayList<>();

    public void updateFromPayload(JsonObject payload) {
        if (payload.has("events"))     updateList(events,     payload.getAsJsonArray("events"));
        if (payload.has("categories")) updateList(categories, payload.getAsJsonArray("categories"));
        if (payload.has("games"))      updateList(games,      payload.getAsJsonArray("games"));
    }

    private void updateList(List<JsonObject> list, JsonArray array) {
        list.clear();
        array.forEach(el -> list.add(el.getAsJsonObject()));
    }

    public Collection<JsonObject> getEvents()     { return List.copyOf(events); }
    public Collection<JsonObject> getCategories() { return List.copyOf(categories); }
    public Collection<JsonObject> getGames()      { return List.copyOf(games); }
}
