package c_y.meknh_ores.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.network.RemovalSyncPayload;
import c_y.meknh_ores.resource.ResourceKind;
import c_y.meknh_ores.resource.ResourceNaming;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runtime cleanup lists.  The mod ships no built-in entries: they are supplied by KubeJS scripts
 * through {@link c_y.meknh_ores.compat.kubejs.MekNHOresKubeJSPlugin}, or optionally by data packs
 * using the historical {@code data/remove/*.json} files.
 *
 * <pre>
 * remove_raw.json     -> del_raw   -> raw items + raw blocks (JEI)
 * remove_ingot.json   -> del_ingot -> ingots, nuggets, dusts + storage blocks (JEI)
 * remove_ores.json    -> del_ores  -> ores (JEI + world generation)
 * remove_natural.json -> no_del    -> exempt mod ids
 * </pre>
 */
public final class RemovalData {
    private static final Source RAW_SOURCE = new Source("remove_raw.json", "del_raw", Mode.RAW);
    private static final Source INGOT_SOURCE = new Source("remove_ingot.json", "del_ingot", Mode.INGOT);
    private static final Source ORES_SOURCE = new Source("remove_ores.json", "del_ores", Mode.ORES);
    private static final Source NO_DEL_SOURCE = new Source("remove_natural.json", "no_del", Mode.NO_DEL);
    private static final List<Source> SOURCES = List.of(RAW_SOURCE, INGOT_SOURCE, ORES_SOURCE, NO_DEL_SOURCE);

    private static final Map<ResourceKind, Set<String>> MATERIALS = new EnumMap<>(ResourceKind.class);
    private static final Set<ResourceLocation> ITEMS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> BLOCKS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> ITEM_TAGS = new LinkedHashSet<>();
    private static final Set<ResourceLocation> BLOCK_TAGS = new LinkedHashSet<>();
    private static final Set<String> NAMESPACES = new LinkedHashSet<>();
    private static final Set<String> EXEMPT_NAMESPACES = new LinkedHashSet<>();

    private RemovalData() {
    }

    /**
     * Optional data-pack support.  The mod itself no longer ships these files; KubeJS scripts should
     * use the direct API instead.
     */
    public static synchronized void reload(ResourceManager resourceManager) {
        for (Source source : SOURCES) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("remove", source.file());
            for (Resource resource : resourceManager.getResourceStack(id)) {
                try (BufferedReader reader = resource.openAsReader()) {
                    parse(reader, source, resource.sourcePackId());
                } catch (Exception exception) {
                    MekNHOres.LOGGER.error("Failed to read removal data {} from {}",
                            id, resource.sourcePackId(), exception);
                }
            }
        }
        logCounts("Merged removal data");
    }

    /**
     * Adds explicit removal entries.  Each entry may be:
     * <ul>
     *     <li>a full item/block id, e.g. {@code thermal:tin_ore};</li>
     *     <li>a tag, e.g. {@code #c:ores/tin};</li>
     *     <li>a namespace wildcard, e.g. {@code thermal:*} or {@code thermal}.</li>
     * </ul>
     * Ids are added to both the item and block lists; ids that do not exist in one registry are
     * simply ignored there.
     */
    public static synchronized void addIds(Iterable<String> ids) {
        for (String id : ids) {
            addId(id);
        }
    }

    public static synchronized void addId(String value) {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            return;
        }

        if (raw.startsWith("#")) {
            ResourceLocation tag = ResourceLocation.tryParse(raw.substring(1));
            if (tag == null) {
                MekNHOres.LOGGER.warn("Ignoring invalid removal tag '{}'", raw);
                return;
            }
            ITEM_TAGS.add(tag);
            BLOCK_TAGS.add(tag);
            return;
        }

        if (raw.endsWith(":*")) {
            addNamespace(raw.substring(0, raw.length() - 2), raw);
            return;
        }

        if (raw.indexOf(':') < 0) {
            addNamespace(raw, raw);
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            MekNHOres.LOGGER.warn("Ignoring invalid removal id '{}'", raw);
            return;
        }
        ITEMS.add(id);
        BLOCKS.add(id);
    }

    private static void addNamespace(String namespace, String original) {
        String normalized = namespace == null ? "" : namespace.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_.-]+")) {
            MekNHOres.LOGGER.warn("Ignoring invalid removal namespace '{}'", original);
            return;
        }
        NAMESPACES.add(normalized);
    }

    public static void addNoDel(String value) {
        addValue(value, NO_DEL_SOURCE, "KubeJS");
    }

    private static void parse(Reader input, Source source, String origin) {
        JsonElement root = JsonParser.parseReader(input);
        JsonElement values = root;
        if (root.isJsonObject() && root.getAsJsonObject().has(source.key())) {
            values = root.getAsJsonObject().get(source.key());
        }
        collect(values, source, origin);
    }

    private static void collect(JsonElement element, Source source, String origin) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                collect(child, source, origin);
            }
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collect(entry.getValue(), source, origin);
            }
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            addValue(element.getAsString(), source, origin);
        }
    }

    private static void addValue(String raw, Source source, String origin) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return;
        }

        if (source.mode() == Mode.NO_DEL) {
            String namespace = value.toLowerCase(Locale.ROOT);
            if (namespace.contains(":")) {
                MekNHOres.LOGGER.warn("Ignoring invalid exempt mod id '{}' in {}", value, origin);
                return;
            }
            EXEMPT_NAMESPACES.add(namespace);
            return;
        }

        if (value.indexOf(':') >= 0) {
            ResourceLocation id = ResourceLocation.tryParse(value);
            if (id == null) {
                MekNHOres.LOGGER.warn("Ignoring invalid removal id '{}' in {}", value, origin);
                return;
            }
            ITEMS.add(id);
            BLOCKS.add(id);
            return;
        }

        String material = ResourceNaming.normalizeMaterial(value);
        if (material.isEmpty()) {
            MekNHOres.LOGGER.warn("Ignoring invalid removal material '{}' in {}", value, origin);
            return;
        }

        switch (source.mode()) {
            case RAW -> {
                addMaterial(ResourceKind.RAW_MATERIAL, material);
                addMaterial(ResourceKind.STORAGE_BLOCK,
                        material.startsWith("raw_") ? material : "raw_" + material);
            }
            case INGOT -> {
                addMaterial(ResourceKind.INGOT, material);
                addMaterial(ResourceKind.STORAGE_BLOCK, material);
                addMaterial(ResourceKind.NUGGET, material);
                addMaterial(ResourceKind.DUST, material);
            }
            case ORES -> addMaterial(ResourceKind.ORE, material);
            case NO_DEL -> {
                // Handled above.
            }
        }
    }

    private static void addMaterial(ResourceKind kind, String material) {
        MATERIALS.computeIfAbsent(kind, ignored -> new LinkedHashSet<>()).add(material);
    }

    public static boolean hasMaterial(ResourceKind kind, String material) {
        Set<String> values = MATERIALS.get(kind);
        return values != null && values.contains(material);
    }

    public static Set<String> materials(ResourceKind kind) {
        Set<String> values = MATERIALS.get(kind);
        return values == null ? Set.of() : Set.copyOf(values);
    }

    public static synchronized boolean hasItem(ResourceLocation id) {
        if (ITEMS.contains(id) || NAMESPACES.contains(id.getNamespace())) {
            return true;
        }
        if (ITEM_TAGS.isEmpty() || !BuiltInRegistries.ITEM.containsKey(id)) {
            return false;
        }
        Holder<Item> holder = BuiltInRegistries.ITEM.wrapAsHolder(BuiltInRegistries.ITEM.get(id));
        for (ResourceLocation tag : ITEM_TAGS) {
            if (holder.is(TagKey.create(Registries.ITEM, tag))) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean hasBlock(ResourceLocation id) {
        if (BLOCKS.contains(id) || NAMESPACES.contains(id.getNamespace())) {
            return true;
        }
        if (BLOCK_TAGS.isEmpty() || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return false;
        }
        Holder<Block> holder = BuiltInRegistries.BLOCK.wrapAsHolder(BuiltInRegistries.BLOCK.get(id));
        for (ResourceLocation tag : BLOCK_TAGS) {
            if (holder.is(TagKey.create(Registries.BLOCK, tag))) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> exemptNamespaces() {
        return Set.copyOf(EXEMPT_NAMESPACES);
    }

    public static RemovalSyncPayload snapshot() {
        Map<ResourceKind, Set<String>> materials = new EnumMap<>(ResourceKind.class);
        for (Map.Entry<ResourceKind, Set<String>> entry : MATERIALS.entrySet()) {
            materials.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return new RemovalSyncPayload(materials, Set.copyOf(ITEMS), Set.copyOf(BLOCKS),
                Set.copyOf(ITEM_TAGS), Set.copyOf(BLOCK_TAGS), Set.copyOf(NAMESPACES),
                Set.copyOf(EXEMPT_NAMESPACES));
    }

    /** Replace the local data with the server snapshot (client-side, after login). */
    public static synchronized void applySnapshot(RemovalSyncPayload payload) {
        MATERIALS.clear();
        ITEMS.clear();
        BLOCKS.clear();
        ITEM_TAGS.clear();
        BLOCK_TAGS.clear();
        NAMESPACES.clear();
        EXEMPT_NAMESPACES.clear();
        for (Map.Entry<ResourceKind, Set<String>> entry : payload.materials().entrySet()) {
            MATERIALS.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }
        ITEMS.addAll(payload.items());
        BLOCKS.addAll(payload.blocks());
        ITEM_TAGS.addAll(payload.itemTags());
        BLOCK_TAGS.addAll(payload.blockTags());
        NAMESPACES.addAll(payload.namespaces());
        EXEMPT_NAMESPACES.addAll(payload.exemptNamespaces());
        logCounts("Applied removal data snapshot");
    }

    private static void logCounts(String prefix) {
        int materialCount = MATERIALS.values().stream().mapToInt(Set::size).sum();
        MekNHOres.LOGGER.info("{}: {} materials, {} items, {} blocks, {} tags, {} namespaces, {} exempt mods",
                prefix, materialCount, ITEMS.size(), BLOCKS.size(),
                ITEM_TAGS.size() + BLOCK_TAGS.size(), NAMESPACES.size(), EXEMPT_NAMESPACES.size());
    }

    private enum Mode {
        RAW,
        INGOT,
        ORES,
        NO_DEL
    }

    private record Source(String file, String key, Mode mode) {
    }
}
