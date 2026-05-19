package fr.like.grackle.storage;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import fr.like.grackle.manager.CategoryManager;
import fr.like.grackle.manager.EventManager;
import fr.like.grackle.model.event.*;
import fr.like.grackle.model.reward.RewardStrategy;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class StorageManager {

    private static final Type CATEGORY_LIST_TYPE = new TypeToken<List<CategoryDto>>() {}.getType();
    private static final Type EVENT_LIST_TYPE     = new TypeToken<List<EventDto>>() {}.getType();

    private final Path dataDir;
    private final Logger logger;
    private final Gson gson;

    public StorageManager(Path dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger  = logger;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeHierarchyAdapter(RewardStrategy.class, new RewardAdapter())
                .create();
    }

    public void init() throws IOException {
        Files.createDirectories(dataDir);
    }

    // -------------------------------------------------------------------------
    // Categories
    // -------------------------------------------------------------------------

    public void saveCategories(CategoryManager manager) {
        List<CategoryDto> dtos = manager.getAll().stream()
                .map(c -> new CategoryDto(c.getName(), c.getDescription()))
                .toList();
        write("categories.json", gson.toJson(dtos));
    }

    public void loadCategories(CategoryManager manager) {
        String raw = read("categories.json");
        if (raw == null) return;
        List<CategoryDto> dtos = gson.fromJson(raw, CATEGORY_LIST_TYPE);
        if (dtos == null) return;
        for (CategoryDto dto : dtos) {
            Category cat = manager.create(dto.name());
            if (dto.description() != null) cat.setDescription(dto.description());
        }
        logger.info("{} catégorie(s) chargée(s).", dtos.size());
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    public void saveEvents(EventManager manager) {
        List<EventDto> dtos = manager.getAll().stream().map(EventDto::from).toList();
        write("events.json", gson.toJson(dtos));
    }

    public void loadEvents(EventManager manager, CategoryManager categoryManager) {
        String raw = read("events.json");
        if (raw == null) return;
        List<EventDto> dtos = gson.fromJson(raw, EVENT_LIST_TYPE);
        if (dtos == null) return;
        for (EventDto dto : dtos) {
            Event event = dto.toEvent(categoryManager);
            manager.register(event);
        }
        logger.info("{} event(s) chargé(s).", dtos.size());
    }

    // -------------------------------------------------------------------------

    private void write(String filename, String content) {
        Path file = dataDir.resolve(filename);
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(content);
        } catch (IOException e) {
            logger.error("Impossible d'écrire {}: {}", filename, e.getMessage());
        }
    }

    private String read(String filename) {
        Path file = dataDir.resolve(filename);
        if (!Files.exists(file)) return null;
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Impossible de lire {}: {}", filename, e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    private record CategoryDto(String name, String description) {}

    private record EventDto(
            String name,
            String type,
            String soloMode,
            int minParticipants,
            int maxParticipants,
            String category,
            String description,
            String server,
            RewardStrategy reward,
            TeamConfigDto teamConfig
    ) {
        static EventDto from(Event e) {
            TeamConfigDto tcDto = null;
            if (e.getTeamConfig() != null) {
                TeamConfig tc = e.getTeamConfig();
                tcDto = new TeamConfigDto(tc.getTeamCount(), tc.isRandomTeams(),
                        tc.getFixedTeamSizes(), tc.getTeamTags());
            }
            return new EventDto(
                    e.getName(),
                    e.getType().name(),
                    e.getSoloMode() != null ? e.getSoloMode().name() : null,
                    e.getMinParticipants(),
                    e.getMaxParticipants(),
                    e.getCategory() != null ? e.getCategory().getName() : null,
                    e.getDescription(),
                    e.getServer(),
                    e.getRewardStrategy(),
                    tcDto
            );
        }

        Event toEvent(CategoryManager categoryManager) {
            Event event = new Event(name, EventType.valueOf(type));
            if (soloMode != null) event.setSoloMode(SoloMode.valueOf(soloMode));
            event.setMinParticipants(minParticipants);
            event.setMaxParticipants(maxParticipants);
            event.setDescription(description);
            if (server != null) event.setServer(server);
            event.setRewardStrategy(reward);
            if (category != null) categoryManager.get(category).ifPresent(event::setCategory);
            if (teamConfig != null) {
                event.setTeamConfig(new TeamConfig(
                        teamConfig.teamCount(),
                        teamConfig.randomTeams(),
                        teamConfig.fixedTeamSizes() != null ? teamConfig.fixedTeamSizes() : Map.of(),
                        teamConfig.teamTags() != null ? teamConfig.teamTags() : Map.of()
                ));
            }
            return event;
        }
    }

    private record TeamConfigDto(
            int teamCount,
            boolean randomTeams,
            Map<String, Integer> fixedTeamSizes,
            Map<String, String> teamTags
    ) {}
}
