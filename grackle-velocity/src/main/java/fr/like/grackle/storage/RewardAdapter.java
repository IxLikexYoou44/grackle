package fr.like.grackle.storage;

import com.google.gson.*;
import fr.like.grackle.model.reward.*;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

public class RewardAdapter implements JsonSerializer<RewardStrategy>, JsonDeserializer<RewardStrategy> {

    @Override
    public JsonElement serialize(RewardStrategy src, Type type, JsonSerializationContext ctx) {
        JsonObject obj = new JsonObject();
        if (src instanceof DegressiveReward r) {
            obj.addProperty("type", "DEGRESSIVE");
            obj.addProperty("max", r.getMax());
            obj.addProperty("range", r.getRange());
            obj.addProperty("participation", r.getParticipation());
        } else if (src instanceof TeamReward r) {
            obj.addProperty("type", "TEAM");
            obj.addProperty("winner", r.getWinnerPoints());
            obj.addProperty("loser", r.getLoserPoints());
        } else if (src instanceof DefinedRankReward r) {
            obj.addProperty("type", "DEFINED");
            obj.addProperty("other", r.getOtherPoints());
            JsonObject ranks = new JsonObject();
            r.getRankPoints().forEach((pos, pts) -> ranks.addProperty(String.valueOf(pos), pts));
            obj.add("ranks", ranks);
        }
        return obj;
    }

    @Override
    public RewardStrategy deserialize(JsonElement json, Type type, JsonDeserializationContext ctx) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String kind = obj.get("type").getAsString();
        return switch (kind) {
            case "DEGRESSIVE" -> new DegressiveReward(
                    obj.get("max").getAsInt(),
                    obj.get("range").getAsInt(),
                    obj.get("participation").getAsInt()
            );
            case "TEAM" -> new TeamReward(
                    obj.get("winner").getAsInt(),
                    obj.get("loser").getAsInt()
            );
            case "DEFINED" -> {
                Map<Integer, Integer> ranks = new TreeMap<>();
                obj.getAsJsonObject("ranks").entrySet()
                        .forEach(e -> ranks.put(Integer.parseInt(e.getKey()), e.getValue().getAsInt()));
                yield new DefinedRankReward(ranks, obj.get("other").getAsInt());
            }
            default -> throw new JsonParseException("Type de récompense inconnu : " + kind);
        };
    }
}
