package c_y.meknh_ores.api.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code data/addon/item/*.json} 的批量物品定义。
 *
 * <pre>{@code
 * {
 *   "tag": "c:aluminum",
 *   "同位体": "all"
 * }
 * }</pre>
 *
 * @param tag       材料 tag，例如 {@code c:aluminum}
 * @param name      显示名（可选），不填时从 tag 路径推导
 * @param forms     要注册的形态；{@code all} 表示全部
 */
public record ItemDefinition(ResourceLocation tag, String name, Set<ItemForm> forms) {

    public static ItemDefinition parse(ResourceLocation fileId, JsonObject json) {
        JsonElement tagElement = json.get("tag");
        if (tagElement == null || !tagElement.isJsonPrimitive() || !tagElement.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Item definition " + fileId + ": 'tag' must be a string");
        }
        ResourceLocation tag = ResourceLocation.tryParse(tagElement.getAsString().trim());
        if (tag == null) {
            throw new IllegalArgumentException("Item definition " + fileId + ": invalid tag '"
                    + tagElement.getAsString() + "' (use an ASCII tag such as c:aluminum)");
        }

        String material = lastPathSegment(tag.getPath());
        String name = json.has("name") ? json.get("name").getAsString()
                : (json.has("名称") ? json.get("名称").getAsString() : defaultName(material));
        Set<ItemForm> forms = parseForms(fileId, json.has("同位体") ? json.get("同位体")
                : (json.has("forms") ? json.get("forms") : json.get("form")));
        return new ItemDefinition(tag, name, forms);
    }

    private static Set<ItemForm> parseForms(ResourceLocation fileId, JsonElement element) {
        Set<ItemForm> forms = new LinkedHashSet<>();
        if (element == null || element.isJsonNull()) {
            forms.addAll(ItemForm.all());
            return forms;
        }
        if (element.isJsonPrimitive()) {
            addForm(fileId, forms, element.getAsString());
            return forms;
        }
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (!entry.isJsonPrimitive()) {
                    throw new IllegalArgumentException("Item definition " + fileId + ": '同位体' entries must be strings");
                }
                addForm(fileId, forms, entry.getAsString());
            }
            return forms;
        }
        throw new IllegalArgumentException("Item definition " + fileId + ": '同位体' must be 'all', a string, or an array");
    }

    private static void addForm(ResourceLocation fileId, Set<ItemForm> forms, String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("all")) {
            forms.addAll(ItemForm.all());
            return;
        }
        ItemForm form = ItemForm.find(normalized);
        if (form == null) {
            throw new IllegalArgumentException("Item definition " + fileId + ": unknown form '" + value + "'");
        }
        forms.add(form);
    }

    private static String lastPathSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String defaultName(String material) {
        String[] parts = material.split("_");
        StringBuilder result = new StringBuilder(material.length());
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    /**
     * 物品注册名：{@code <形态>_<材料>}，例如 {@code ingot_aluminum}。
     */
    public String itemPath(ItemForm form) {
        return form.tag() + "_" + material();
    }

    /** 材料名（tag 路径最后一段），贴图目录按 {@code textures/item/<形态>/<材料>.png} 组织。 */
    public String material() {
        return lastPathSegment(this.tag.getPath());
    }
}
