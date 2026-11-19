package c_y.meknh_ores.api.item;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * 物品形态（{@code 同位体}）。
 *
 * <p>形态本身就是 tag 形式；新增形态只需要在静态块或外部调用 {@link #register}，
 * 不必改批量注册逻辑。贴图按 {@code textures/item/<形态>/<材料>.png} 约定加载。</p>
 */
public record ItemForm(String tag, String translationKey, String nameKey) {

    private static final Map<String, ItemForm> FORMS = new LinkedHashMap<>();

    public static final ItemForm INGOT = register("ingot", "item.meknh_ores.form.ingot");
    public static final ItemForm DUST = register("dust", "item.meknh_ores.form.dust");
    public static final ItemForm DIRTY_DUST = register("dirty_dust", "item.meknh_ores.form.dirty_dust",
            "item.meknh_ores.form.name.dirty_dust");
    public static final ItemForm NUGGET = register("nugget", "item.meknh_ores.form.nugget");
    public static final ItemForm CLUMP = register("clump", "item.meknh_ores.form.clump");
    public static final ItemForm CRYSTAL = register("crystal", "item.meknh_ores.form.crystal");
    public static final ItemForm SHARD = register("shard", "item.meknh_ores.form.shard");
    public static final ItemForm GEAR = register("gear", "item.meknh_ores.form.gear");
    public static final ItemForm ROTOR = register("rotor", "item.meknh_ores.form.rotor");
    public static final ItemForm PLATE = register("plate", "item.meknh_ores.form.plate");
    public static final ItemForm ROD = register("rod", "item.meknh_ores.form.rod");
    public static final ItemForm WIRE_FINE = register("wire_fine", "item.meknh_ores.form.wire_fine");
    public static final ItemForm RAW = register("raw", "item.meknh_ores.form.raw",
            "item.meknh_ores.form.name.raw");
    public static final ItemForm HOT_INGOT = register("hot_ingot", "item.meknh_ores.form.hot_ingot",
            "item.meknh_ores.form.name.hot_ingot");

    public static synchronized ItemForm register(String tag, String translationKey) {
        return register(tag, translationKey, "item.meknh_ores.form.name");
    }

    public static synchronized ItemForm register(String tag, String translationKey, String nameKey) {
        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        ItemForm form = new ItemForm(normalized, translationKey, nameKey);
        ItemForm previous = FORMS.putIfAbsent(normalized, form);
        if (previous != null) {
            throw new IllegalStateException("Item form '" + normalized + "' has already been registered");
        }
        return form;
    }

    @Nullable
    public static synchronized ItemForm find(String tag) {
        return FORMS.get(tag.trim().toLowerCase(Locale.ROOT));
    }

    public static synchronized Collection<ItemForm> all() {
        return java.util.List.copyOf(FORMS.values());
    }

    /**
     * Conventional {@code c:} tag folder for this form.  The plural forms follow the existing
     * NeoForge/Mekanism conventions (plates, rods, gears, dirty_dusts, ...).
     */
    public String tagFolder() {
        return switch (tag) {
            case "ingot" -> "ingots";
            case "dust" -> "dusts";
            case "dirty_dust" -> "dirty_dusts";
            case "nugget" -> "nuggets";
            case "clump" -> "clumps";
            case "crystal" -> "crystals";
            case "shard" -> "shards";
            case "gear" -> "gears";
            case "rotor" -> "rotors";
            case "plate" -> "plates";
            case "rod" -> "rods";
            case "wire_fine" -> "wires/fine";
            case "raw" -> "raw_materials";
            case "hot_ingot" -> "hot_ingots";
            default -> tag + "s";
        };
    }
}
