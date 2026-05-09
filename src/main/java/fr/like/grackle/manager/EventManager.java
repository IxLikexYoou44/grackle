package fr.like.grackle.manager;

import fr.like.grackle.model.event.Event;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EventManager {

    private final Map<String, Event> events = new HashMap<>();

    public boolean register(Event event) {
        if (events.containsKey(event.getName().toLowerCase())) return false;
        events.put(event.getName().toLowerCase(), event);
        return true;
    }

    public Optional<Event> get(String name) {
        return Optional.ofNullable(events.get(name.toLowerCase()));
    }

    public boolean remove(String name) {
        return events.remove(name.toLowerCase()) != null;
    }

    public Collection<Event> getAll() {
        return events.values();
    }
}
