package c_y.meknh_ores.api.chemical;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/**
 * 一个材料的 Slurry 定义。会注册为 {@code dirty_<id>} 和 {@code clean_<id>} 两个 Mekanism 化学品。
 */
public record SlurryDefinition(ResourceLocation id, int tint) {

    public static SlurryDefinition parse(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = fileId;
        if (json.has("id")) {
            id = ResourceLocation.tryParse(json.get("id").getAsString());
            if (id == null) {
                throw new IllegalArgumentException("Slurry definition " + fileId + ": invalid id");
            }
        }
        JsonElement tintElement = json.has("tint") ? json.get("tint")
                : (json.has("颜色") ? json.get("颜色") : null);
        int tint = parseColor(fileId, tintElement);
        return new SlurryDefinition(id, tint);
    }

    private static int parseColor(ResourceLocation fileId, JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Slurry definition " + fileId + ": 'tint' is required");
        }
        if (element.getAsJsonPrimitive().isNumber()) {
            return element.getAsInt() & 0xFFFFFF;
        }
        String value = element.getAsString().trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x")) {
            value = value.substring(2);
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Slurry definition " + fileId + ": invalid tint", e);
        }
    }
}
