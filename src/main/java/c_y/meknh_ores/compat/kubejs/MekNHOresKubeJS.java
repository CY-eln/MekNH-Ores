package c_y.meknh_ores.compat.kubejs;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.data.RemovalData;
import c_y.meknh_ores.network.RemovalNetwork;
import c_y.meknh_ores.resource.UnifiedResourceIndex;
import c_y.meknh_ores.worldgen.vein.OreVeinBuilder;
import c_y.meknh_ores.worldgen.vein.WorldGenLayerBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Object exposed to KubeJS as {@code MekNHOres}.
 *
 * <p>Cleanup is id-based now: every entry is an item/block id, a tag ({@code #namespace:path}) or
 * a namespace wildcard ({@code namespace:*} / {@code namespace}).  Listed ids are hidden from JEI
 * and their original world generation is removed.  Vein takeover is independent and unchanged.</p>
 */
public final class MekNHOresKubeJS {

    /**
     * Primary cleanup entry point.  Accepts an array, a list, or varargs:
     * {@code MekNHOres.del(['mod:ore', '#c:ores/tin'])}, or
     * {@code MekNHOres.del('mod:ore', '#c:ores/tin')}.
     */
    public void del(Object... entries) {
        removeEntries(entries);
    }

    /** Alias for {@link #del(Object...)}. */
    public void remove(Object... entries) {
        removeEntries(entries);
    }

    public void noDel(Object... namespaces) {
        addExemptNamespaces(namespaces);
    }

    public OreVeinBuilder vein(String id) {
        return new OreVeinBuilder(id);
    }

    public WorldGenLayerBuilder layer(String id) {
        return new WorldGenLayerBuilder(id);
    }

    private static void removeEntries(Object[] entries) {
        List<String> values = new ArrayList<>();
        collect(entries, values);
        if (values.isEmpty()) {
            return;
        }
        RemovalData.addIds(values);
        refresh("del", values.size());
    }

    private static void addExemptNamespaces(Object[] namespaces) {
        List<String> values = new ArrayList<>();
        collect(namespaces, values);
        if (values.isEmpty()) {
            return;
        }
        for (String value : values) {
            RemovalData.addNoDel(value);
        }
        refresh("no_del", values.size());
    }

    /** Flattens JS arrays/lists and Java arrays into a list of strings. */
    private static void collect(Object value, List<String> output) {
        if (value == null) {
            return;
        }
        if (value instanceof Object[] array) {
            for (Object element : array) {
                collect(element, output);
            }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                collect(element, output);
            }
            return;
        }
        String text = value.toString();
        if (!text.isBlank()) {
            output.add(text.trim());
        }
    }

    private static void refresh(String key, int count) {
        UnifiedResourceIndex.invalidate();
        RemovalNetwork.broadcastIfServer();
        MekNHOres.LOGGER.info("KubeJS cleanup entries: {} = {}", key, count);
    }
}
