package c_y.meknh_ores.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import c_y.meknh_ores.MekNHOres;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A tiny generated data pack.  It exists only in memory and exposes conventional {@code c:} tags
 * built from the actual registries at reload time.  Because it is a normal pack, tag merging,
 * {@code replace} handling and client/server syncing are handled by vanilla.
 */
public final class UnifiedTagPack implements PackResources {
    public static final String PACK_ID = "meknh_ores_unified_tags";
    public static final int DATA_PACK_FORMAT = 48;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String NAMESPACE = "c";

    private final PackLocationInfo location;
    private final Map<String, byte[]> resources;

    public UnifiedTagPack(PackLocationInfo location) {
        this.location = location;
        this.resources = buildResources();
        MekNHOres.LOGGER.info("Generated {} unified resource tag files", this.resources.size());
    }

    private static Map<String, byte[]> buildResources() {
        Map<String, Set<String>> values = new TreeMap<>();
        collectItems(values);
        collectBlocks(values);

        Map<String, byte[]> result = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : values.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            JsonObject root = new JsonObject();
            root.addProperty("replace", false);
            JsonArray array = new JsonArray();
            for (String value : entry.getValue()) {
                array.add(value);
            }
            root.add("values", array);
            result.put(entry.getKey(), GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
        }
        return Map.copyOf(result);
    }

    private static void collectItems(Map<String, Set<String>> values) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ResourceNaming.parse(id).ifPresent(parsed -> {
                String parent = "tags/item/" + parsed.kind().folder() + ".json";
                String child = "tags/item/" + parsed.kind().folder() + "/" + parsed.material() + ".json";
                values.computeIfAbsent(parent, ignored -> new TreeSet<>()).add(id.toString());
                values.computeIfAbsent(child, ignored -> new TreeSet<>()).add(id.toString());
            });
        }
    }

    private static void collectBlocks(Map<String, Set<String>> values) {
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            ResourceNaming.parse(id).ifPresent(parsed -> {
                String parent = "tags/block/" + parsed.kind().folder() + ".json";
                String child = "tags/block/" + parsed.kind().folder() + "/" + parsed.material() + ".json";
                values.computeIfAbsent(parent, ignored -> new TreeSet<>()).add(id.toString());
                values.computeIfAbsent(child, ignored -> new TreeSet<>()).add(id.toString());
            });
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        String joined = String.join("/", path);
        if ("pack.mcmeta".equals(joined)) {
            String json = """
                    {
                      "pack": {
                        "pack_format": %d,
                        "description": "MekNH Ores generated unified resource tags"
                      }
                    }
                    """.formatted(DATA_PACK_FORMAT);
            return () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        if (type != PackType.SERVER_DATA || !NAMESPACE.equals(id.getNamespace())) {
            return null;
        }
        byte[] data = resources.get(id.getPath());
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !NAMESPACE.equals(namespace)) {
            return;
        }
        String prefix = path.isEmpty() || path.endsWith("/") ? path : path + "/";
        for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NAMESPACE, entry.getKey());
            byte[] data = entry.getValue();
            output.accept(id, () -> new ByteArrayInputStream(data));
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(NAMESPACE) : Set.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer == PackMetadataSection.TYPE) {
            return (T) new PackMetadataSection(
                    Component.literal("MekNH Ores generated unified resource tags"),
                    DATA_PACK_FORMAT
            );
        }
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return location;
    }

    @Override
    public void close() {
        // Nothing to close: all resources are in-memory byte arrays.
    }
}
