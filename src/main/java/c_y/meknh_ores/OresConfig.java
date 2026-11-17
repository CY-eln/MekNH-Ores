package c_y.meknh_ores;

import c_y.meknh_ores.data.RemovalData;
import c_y.meknh_ores.resource.ResourceKind;
import c_y.meknh_ores.resource.ResourceNaming;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Common configuration.  Everything in here is deliberately pack-author friendly: no item registry
 * names are hard-coded, only namespaces and a handful of switches.
 */
public final class OresConfig {
    private static final List<String> DEFAULT_EXEMPT_MODS = List.of(
            "minecraft",
            "mekanism",
            "mekanismgenerators",
            "ad_astra"
    );

    /**
     * Only these materials are treated as duplicates.  Everything else — including fantasy metals
     * and alloys such as aether, quantum_alloy and entro_infused — is left completely alone.
     */
    private static final List<String> DEFAULT_CLEANUP_MATERIALS = List.of();

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXEMPT_MODS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXEMPT_ITEMS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CANONICAL_PRIORITY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CLEANUP_MATERIALS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CLEANUP_BLOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CLEANUP_ITEMS;

    public static final ModConfigSpec.BooleanValue UNIFY_TAGS;
    public static final ModConfigSpec.BooleanValue DISABLE_FOREIGN_ORE_GENERATION;
    public static final ModConfigSpec.BooleanValue ENABLE_VEIN_TAKEOVER;
    public static final ModConfigSpec.IntValue VEIN_GRID_SIZE;
    public static final ModConfigSpec.IntValue VEIN_RANDOM_OFFSET;
    public static final ModConfigSpec.IntValue VEIN_CACHE_SIZE;
    public static final ModConfigSpec.BooleanValue VEIN_INDICATORS;
    public static final ModConfigSpec.BooleanValue HIDE_ORES;
    public static final ModConfigSpec.BooleanValue HIDE_RAW_MATERIALS;
    public static final ModConfigSpec.BooleanValue HIDE_INGOTS;
    public static final ModConfigSpec.BooleanValue HIDE_NUGGETS;
    public static final ModConfigSpec.BooleanValue HIDE_DUSTS;
    public static final ModConfigSpec.BooleanValue HIDE_STORAGE_BLOCKS;
    public static final ModConfigSpec.BooleanValue HIDE_ONLY_IF_CANONICAL_EXISTS;
    public static final ModConfigSpec.BooleanValue LOG_HIDDEN_ITEMS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("unification");
        builder.comment(
                "Mod namespaces that are allowed to keep their ores and ingots.",
                "Mekanism and Ad Astra are exempt by default.  'minecraft' is always exempt even",
                "if it is removed from this list."
        );
        EXEMPT_MODS = builder.defineListAllowEmpty(
                "exempt_mods",
                DEFAULT_EXEMPT_MODS,
                () -> "example_mod",
                OresConfig::isValidNamespace
        );

        builder.comment(
                "Individual item ids that must never be hidden or treated as foreign,",
                "even if they are listed in MekNHOres.del or their material is in cleanup_materials.",
                "For example 'create:crushed_raw_iron'."
        );
        EXEMPT_ITEMS = builder.defineListAllowEmpty(
                "exempt_items",
                List.of(),
                () -> "minecraft:air",
                OresConfig::isValidResourceLocation
        );

        builder.comment(
                "Additional whitelisted materials, used as a config fallback.",
                "The primary source is KubeJS server scripts:",
                "  MekNHOres.del(['thermal:tin_ore', '#c:ores/tin']) -> JEI + world generation",
                "  MekNHOres.del('thermal:*')                           -> whole mod namespace",
                "  MekNHOres.noDel('mekanism')                          -> exempt mod ids",
                "Only whitelisted materials have their foreign ore generation removed and their",
                "foreign ingots/ores/raw materials hidden in JEI.  Everything else is left alone.",
                "Use base material names; raw_tin/raw_tin_block are matched against 'tin'."
        );
        CLEANUP_MATERIALS = builder.defineListAllowEmpty(
                "cleanup_materials",
                DEFAULT_CLEANUP_MATERIALS,
                () -> "tin",
                OresConfig::isValidMaterial
        );

        builder.comment(
                "Additional explicit ore/raw block ids to clean up.",
                "Useful for unusual modded ore ids."
        );
        CLEANUP_BLOCKS = builder.defineListAllowEmpty(
                "cleanup_blocks",
                List.of(),
                () -> "example_mod:strange_tin_ore",
                OresConfig::isValidResourceLocation
        );

        builder.comment(
                "Additional explicit item ids to hide in JEI.",
                "Useful for unusual modded ingot/raw item ids."
        );
        CLEANUP_ITEMS = builder.defineListAllowEmpty(
                "cleanup_items",
                List.of(),
                () -> "example_mod:strange_tin_ingot",
                OresConfig::isValidResourceLocation
        );

        builder.comment(
                "Order used to choose the representative item for a material.",
                "Only namespaces present in the pack matter; the first matching namespace wins."
        );
        CANONICAL_PRIORITY = builder.defineListAllowEmpty(
                "canonical_priority",
                List.of("mekanism", "ad_astra", "minecraft"),
                () -> "minecraft",
                OresConfig::isValidNamespace
        );

        builder.comment(
                "Generate a built-in data pack that adds every recognised ore/ingot/etc. item",
                "and block to the conventional c: tags.  This is what makes foreign items",
                "usable in tag-based recipes even when they are hidden or no longer generated."
        );
        UNIFY_TAGS = builder.define("unify_tags", true);

        builder.comment(
                "Remove biome features that place ore blocks from non-exempt namespaces when the",
                "ore material is in cleanup_materials or the block id is in cleanup_blocks.",
                "Non-whitelisted materials are never removed, regardless of their name.",
                "Exempt mods are never touched."
        );
        DISABLE_FOREIGN_ORE_GENERATION = builder.define("disable_foreign_ore_generation", true);

        builder.comment(
                "Take over world generation for ores used by KubeJS ore veins.",
                "Only ore blocks explicitly used in a vein are suppressed from their original",
                "generation and replaced by the custom vein feature.  Unlisted ores are untouched."
        );
        ENABLE_VEIN_TAKEOVER = builder.define("enable_vein_takeover", true);

        builder.comment(
                "Distance, in chunks, between ore vein grid nodes.",
                "3 matches GregTech's default: at most one vein per applicable layer per 3x3 chunks.",
                "Because every vein is generated on this grid, this also controls total vein density."
        );
        VEIN_GRID_SIZE = builder.defineInRange("vein_grid_size", 3, 1, 32);
        builder.comment(
                "Maximum random offset, in blocks, applied to a vein center inside its grid node.",
                "GregTech's default is 12. 0 keeps every vein exactly at the node's chunk center."
        );
        VEIN_RANDOM_OFFSET = builder.defineInRange("vein_random_offset", 12, 0, 512);
        builder.comment(
                "Maximum number of generated veins kept in the per-level cache.",
                "Increase this if you use very large custom veins or a large grid/offset."
        );
        VEIN_CACHE_SIZE = builder.defineInRange("vein_cache_size", 1024, 64, 65536);
        builder.comment(
                "Generate GregTech-style surface indicators above/below veins.",
                "Individual veins still need a surfaceIndicator(...) definition; this is a global switch."
        );
        VEIN_INDICATORS = builder.define("vein_indicators", true);
        builder.pop();

        builder.push("jei");
        builder.comment("Hide whitelisted, non-exempt ore blocks/items in JEI.");
        HIDE_ORES = builder.define("hide_ores", true);
        builder.comment("Hide whitelisted, non-exempt raw ore items (raw_iron, raw_tin, ...).");
        HIDE_RAW_MATERIALS = builder.define("hide_raw_materials", true);
        builder.comment("Hide whitelisted, non-exempt ingots in JEI.");
        HIDE_INGOTS = builder.define("hide_ingots", true);
        builder.comment("Hide whitelisted, non-exempt nuggets in JEI.");
        HIDE_NUGGETS = builder.define("hide_nuggets", true);
        builder.comment("Hide whitelisted, non-exempt dusts in JEI.");
        HIDE_DUSTS = builder.define("hide_dusts", true);
        builder.comment("Hide whitelisted, non-exempt storage blocks in JEI.");
        HIDE_STORAGE_BLOCKS = builder.define("hide_storage_blocks", true);
        builder.comment(
                "When true, an item is only hidden if an exempt canonical item exists for the",
                "same material.  Useful if the pack contains unique materials that only one",
                "non-exempt mod provides."
        );
        HIDE_ONLY_IF_CANONICAL_EXISTS = builder.define("hide_only_if_canonical_exists", false);
        builder.comment("Log the total number of stacks hidden from JEI.");
        LOG_HIDDEN_ITEMS = builder.define("log_hidden_items", true);
        builder.pop();

        SPEC = builder.build();
    }

    private OresConfig() {
    }

    /** A stable, lower-case set of exempt namespaces. */
    public static Set<String> exemptNamespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        namespaces.add("minecraft");
        namespaces.add(MekNHOres.MODID);
        namespaces.addAll(RemovalData.exemptNamespaces());
        for (String value : getStringList(EXEMPT_MODS, DEFAULT_EXEMPT_MODS)) {
            String namespace = normalizeNamespace(value);
            if (!namespace.isEmpty()) {
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }

    public static boolean isExemptNamespace(String namespace) {
        return exemptNamespaces().contains(namespace.toLowerCase(Locale.ROOT));
    }

    public static boolean isExemptItem(ResourceLocation id) {
        if (isExemptNamespace(id.getNamespace())) {
            return true;
        }
        return isExplicitlyExemptItem(id);
    }

    /** Only the {@code exempt_items} list, without the namespace exemption. */
    public static boolean isExplicitlyExemptItem(ResourceLocation id) {
        for (String value : getStringList(EXEMPT_ITEMS, List.of())) {
            ResourceLocation exempt = ResourceLocation.tryParse(value);
            if (id.equals(exempt)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hide(ResourceKind kind) {
        return switch (kind) {
            case ORE -> getBoolean(HIDE_ORES, true);
            case RAW_MATERIAL -> getBoolean(HIDE_RAW_MATERIALS, true);
            case INGOT -> getBoolean(HIDE_INGOTS, true);
            case NUGGET -> getBoolean(HIDE_NUGGETS, false);
            case DUST -> getBoolean(HIDE_DUSTS, false);
            case STORAGE_BLOCK -> getBoolean(HIDE_STORAGE_BLOCKS, false);
        };
    }

    public static boolean hideOnlyIfCanonicalExists() {
        return getBoolean(HIDE_ONLY_IF_CANONICAL_EXISTS, false);
    }

    /**
     * Whitelist check: only listed materials (or their base form, e.g. raw_tin → tin) are cleaned.
     * The JSON data files are kind-specific; the config list is a global additive override.
     */
    public static boolean isCleanupMaterial(ResourceKind kind, String material) {
        String normalized = ResourceNaming.normalizeMaterial(material);
        if (normalized.isEmpty()) {
            return false;
        }
        if (containsMaterial(CLEANUP_MATERIALS, normalized) || RemovalData.hasMaterial(kind, normalized)) {
            return true;
        }
        String base = ResourceNaming.baseMaterial(normalized);
        if (base.isEmpty() || base.equals(normalized)) {
            return false;
        }
        return containsMaterial(CLEANUP_MATERIALS, base) || RemovalData.hasMaterial(kind, base);
    }

    /** True when the material comes from the four data files, not just the config list. */
    public static boolean isDataCleanupMaterial(ResourceKind kind, String material) {
        String normalized = ResourceNaming.normalizeMaterial(material);
        if (normalized.isEmpty()) {
            return false;
        }
        if (RemovalData.hasMaterial(kind, normalized)) {
            return true;
        }
        String base = ResourceNaming.baseMaterial(normalized);
        return !base.isEmpty() && !base.equals(normalized) && RemovalData.hasMaterial(kind, base);
    }

    /** Resource forms covered by the cleanup whitelist. */
    public static boolean isCleanupResourceKind(ResourceKind kind) {
        return switch (kind) {
            case INGOT, ORE, RAW_MATERIAL, STORAGE_BLOCK, NUGGET, DUST -> true;
        };
    }

    public static boolean isExplicitCleanupBlock(ResourceLocation id) {
        return containsResourceLocation(CLEANUP_BLOCKS, id) || RemovalData.hasBlock(id);
    }

    public static boolean isExplicitCleanupItem(ResourceLocation id) {
        return containsResourceLocation(CLEANUP_ITEMS, id) || RemovalData.hasItem(id);
    }

    /** Normalized whitelist entries plus base forms, used for feature-id matching. */
    public static List<String> cleanupMaterials() {
        List<String> result = new ArrayList<>();
        for (String value : getStringList(CLEANUP_MATERIALS, DEFAULT_CLEANUP_MATERIALS)) {
            String material = ResourceNaming.normalizeMaterial(value);
            if (material.isEmpty()) {
                continue;
            }
            if (!result.contains(material)) {
                result.add(material);
            }
            String base = ResourceNaming.baseMaterial(material);
            if (!base.isEmpty() && !result.contains(base)) {
                result.add(base);
            }
        }
        for (String material : RemovalData.materials(ResourceKind.ORE)) {
            if (!result.contains(material)) {
                result.add(material);
            }
            String base = ResourceNaming.baseMaterial(material);
            if (!base.isEmpty() && !result.contains(base)) {
                result.add(base);
            }
        }
        return result;
    }

    public static boolean unifyTags() {
        return getBoolean(UNIFY_TAGS, true);
    }

    public static boolean disableForeignOreGeneration() {
        return getBoolean(DISABLE_FOREIGN_ORE_GENERATION, true);
    }

    public static boolean enableVeinTakeover() {
        return getBoolean(ENABLE_VEIN_TAKEOVER, true);
    }

    public static int veinGridSize() {
        return SPEC.isLoaded() ? VEIN_GRID_SIZE.get() : 3;
    }

    public static int veinRandomOffset() {
        return SPEC.isLoaded() ? VEIN_RANDOM_OFFSET.get() : 12;
    }

    public static int veinCacheSize() {
        return SPEC.isLoaded() ? VEIN_CACHE_SIZE.get() : 1024;
    }

    public static boolean veinIndicators() {
        return SPEC.isLoaded() ? VEIN_INDICATORS.get() : true;
    }

    public static boolean logHiddenItems() {
        return getBoolean(LOG_HIDDEN_ITEMS, true);
    }

    /** Lower index means higher priority.  Unknown namespaces sort after all configured ones. */
    public static int canonicalPriority(String namespace) {
        List<? extends String> values = getStringList(CANONICAL_PRIORITY, List.of("mekanism", "ad_astra", "minecraft"));
        int index = 0;
        for (String value : values) {
            if (normalizeNamespace(value).equals(namespace)) {
                return index;
            }
            index++;
        }
        return Integer.MAX_VALUE;
    }

    public static List<String> canonicalPriorityList() {
        List<? extends String> values = getStringList(CANONICAL_PRIORITY, List.of("mekanism", "ad_astra", "minecraft"));
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            String namespace = normalizeNamespace(value);
            if (!namespace.isEmpty() && !result.contains(namespace)) {
                result.add(namespace);
            }
        }
        return result;
    }

    private static boolean getBoolean(ModConfigSpec.BooleanValue value, boolean fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    private static List<? extends String> getStringList(ModConfigSpec.ConfigValue<List<? extends String>> value,
                                                        List<String> fallback) {
        return SPEC.isLoaded() ? value.get() : fallback;
    }

    private static boolean isValidNamespace(Object value) {
        return value instanceof String string && !normalizeNamespace(string).isEmpty()
                && normalizeNamespace(string).matches("[a-z0-9_.-]+");
    }

    private static boolean isValidResourceLocation(Object value) {
        return value instanceof String string && ResourceLocation.tryParse(string) != null;
    }

    private static boolean isValidMaterial(Object value) {
        return value instanceof String string && !ResourceNaming.normalizeMaterial(string).isEmpty();
    }

    private static boolean containsMaterial(ModConfigSpec.ConfigValue<List<? extends String>> values,
                                            String material) {
        for (String value : getStringList(values, List.of())) {
            if (ResourceNaming.normalizeMaterial(value).equals(material)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsResourceLocation(ModConfigSpec.ConfigValue<List<? extends String>> values,
                                                    ResourceLocation id) {
        for (String value : getStringList(values, List.of())) {
            if (id.equals(ResourceLocation.tryParse(value))) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeNamespace(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
