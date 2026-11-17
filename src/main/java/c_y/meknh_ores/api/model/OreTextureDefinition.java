package c_y.meknh_ores.api.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/**
 * 一个矿石材质允许使用的底图列表。
 *
 * <pre>{@code
 * {
 *   "material": "aluminum",
 *   "bases": ["stone", "deepslate"]
 * }
 * }</pre>
 *
 * <p>没有配置的材料默认合并所有底图；配置后只生成列出的底图。
 * 不存在的底图会被跳过，因此没有深板岩形态的矿只写 {@code ["stone"]} 即可。</p>
 */
public record OreTextureDefinition(String material, List<String> bases,
                                   @org.jetbrains.annotations.Nullable ResourceLocation drop) {

    public static OreTextureDefinition parse(ResourceLocation fileId, JsonObject json) {
        String material = json.has("material") ? json.get("material").getAsString()
                : fileId.getPath();
        material = material.toLowerCase(Locale.ROOT);
        JsonElement element = json.has("bases") ? json.get("bases") : json.get("base");
        List<String> bases = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            bases.add("all");
        } else if (element.isJsonPrimitive()) {
            addBase(bases, element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (!entry.isJsonPrimitive()) {
                    throw new IllegalArgumentException("Ore texture " + fileId + ": 'bases' entries must be strings");
                }
                addBase(bases, entry.getAsString());
            }
        } else {
            throw new IllegalArgumentException("Ore texture " + fileId + ": 'bases' must be a string or array");
        }
        if (bases.isEmpty()) {
            throw new IllegalArgumentException("Ore texture " + fileId + ": 'bases' must not be empty");
        }
        ResourceLocation drop = null;
        JsonElement dropElement = json.has("drop") ? json.get("drop") : json.get("掉落");
        if (dropElement != null && dropElement.isJsonPrimitive()) {
            drop = ResourceLocation.tryParse(dropElement.getAsString());
            if (drop == null) {
                throw new IllegalArgumentException("Ore texture " + fileId + ": invalid 'drop' id");
            }
        }
        return new OreTextureDefinition(material, List.copyOf(bases), drop);
    }

    private static void addBase(List<String> bases, String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty() && !bases.contains(normalized)) {
            bases.add(normalized);
        }
    }
}
