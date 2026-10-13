package com.andrewparry.seamlessworldwrap;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A world's wrap choices, chosen when the world is created and saved with it. The Nether always
 * follows the Overworld at 1/8 size; the End has its own settings (by default the same as the
 * Overworld). Null means that dimension doesn't wrap.
 */
public record WorldWrapOptions(@Nullable WrapSettings overworld, @Nullable WrapSettings end) {

    public static final WorldWrapOptions NONE = new WorldWrapOptions(null, null);

    public static WorldWrapOptions defaults() {
        WrapSettings overworld = WrapSettings.of(true, true, 64, 64);
        return new WorldWrapOptions(overworld, overworld);
    }

    public Map<ResourceKey<Level>, WrapSettings> byDimension() {
        Map<ResourceKey<Level>, WrapSettings> map = new HashMap<>();
        if (overworld != null) {
            map.put(Level.OVERWORLD, overworld);
            map.put(Level.NETHER, overworld.nether());
        }
        if (end != null) {
            map.put(Level.END, end);
        }
        return map;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("version", 1);
        json.add("overworld", settingsToJson(overworld));
        json.add("end", settingsToJson(end));
        return json;
    }

    public static WorldWrapOptions fromJson(JsonObject json) {
        return new WorldWrapOptions(settingsFromJson(json.get("overworld")), settingsFromJson(json.get("end")));
    }

    private static JsonElement settingsToJson(@Nullable WrapSettings settings) {
        if (settings == null) {
            return JsonNull.INSTANCE;
        }
        JsonObject json = new JsonObject();
        json.addProperty("wrapEastWest", settings.wrapsX());
        json.addProperty("wrapNorthSouth", settings.wrapsZ());
        json.addProperty("widthChunks", settings.widthChunks());
        json.addProperty("lengthChunks", settings.heightChunks());
        json.addProperty("blendWidth", settings.blendWidth());
        return json;
    }

    @Nullable
    private static WrapSettings settingsFromJson(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject json = element.getAsJsonObject();
        return new WrapSettings(
                json.get("wrapEastWest").getAsBoolean(),
                json.get("wrapNorthSouth").getAsBoolean(),
                json.get("widthChunks").getAsInt(),
                json.get("lengthChunks").getAsInt(),
                json.get("blendWidth").getAsInt());
    }
}
