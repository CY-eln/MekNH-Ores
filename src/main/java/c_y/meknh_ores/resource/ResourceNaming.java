package c_y.meknh_ores.resource;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Optional;

/**
 * Name-based fallback used both for the generated tag data pack and for JEI filtering.
 *
 * <p>It intentionally only understands well-established resource naming patterns; anything that
 * does not match can still be unified by a datapack tag and will then be picked up through the tag
 * scan in {@link UnifiedResourceIndex}.</p>
 */
public final class ResourceNaming {
    private static final String[] ORE_PREFIXES = {
            "deepslate_", "nether_", "end_"
    };

    private static final String[] BASE_PREFIXES = {
            "raw_", "refined_", "purified_", "pure_", "molten_", "hot_",
            "liquid_", "solid_", "clean_", "dirty_", "crushed_"
    };

    private static final String[] BASE_SUFFIXES = {
            "_metal", "_alloy", "_compound", "_infused", "_crystal",
            "_ore", "_raw", "_block"
    };

    private ResourceNaming() {
    }

    public static Optional<ParsedResource> parse(ResourceLocation id) {
        String path = id.getPath();
        if (path.indexOf('/') >= 0) {
            return Optional.empty();
        }

        if (path.endsWith("_ore")) {
            String material = stripOrePrefixes(path.substring(0, path.length() - "_ore".length()));
            return parsed(ResourceKind.ORE, material);
        }

        // Some mods use ids such as silver_ore_deepslate instead of deepslate_silver_ore.
        int embeddedOre = path.indexOf("_ore_");
        if (embeddedOre > 0) {
            String material = stripOrePrefixes(path.substring(0, embeddedOre));
            return parsed(ResourceKind.ORE, material);
        }

        if (path.startsWith("raw_")) {
            String tail = path.substring("raw_".length());
            if (tail.endsWith("_block")) {
                String material = tail.substring(0, tail.length() - "_block".length());
                return parsed(ResourceKind.STORAGE_BLOCK, material.isEmpty() ? "" : "raw_" + material);
            }
            return parsed(ResourceKind.RAW_MATERIAL, tail);
        }

        if (path.endsWith("_raw")) {
            return parsed(ResourceKind.RAW_MATERIAL, path.substring(0, path.length() - "_raw".length()));
        }

        if (path.startsWith("ingot_")) {
            return parsed(ResourceKind.INGOT, path.substring("ingot_".length()));
        }
        if (path.endsWith("_ingot")) {
            return parsed(ResourceKind.INGOT, path.substring(0, path.length() - "_ingot".length()));
        }

        if (path.startsWith("nugget_")) {
            return parsed(ResourceKind.NUGGET, path.substring("nugget_".length()));
        }
        if (path.endsWith("_nugget")) {
            return parsed(ResourceKind.NUGGET, path.substring(0, path.length() - "_nugget".length()));
        }

        if (path.startsWith("dust_")) {
            return parsed(ResourceKind.DUST, path.substring("dust_".length()));
        }
        if (path.endsWith("_dust")) {
            return parsed(ResourceKind.DUST, path.substring(0, path.length() - "_dust".length()));
        }

        if (path.startsWith("block_raw_")) {
            return parsed(ResourceKind.STORAGE_BLOCK, "raw_" + path.substring("block_raw_".length()));
        }

        // Deliberately no generic "<material>_block" parsing here.  IDs such as note_block or
        // command_block are not resource storage blocks, and adding them to c:storage_blocks would
        // pollute a very broad tag.  Storage blocks are still picked up from loaded c: tags by
        // UnifiedResourceIndex, and raw ore blocks are handled above.
        return Optional.empty();
    }

    private static String stripOrePrefixes(String material) {
        String result = material;
        boolean changed;
        do {
            changed = false;
            for (String prefix : ORE_PREFIXES) {
                if (result.startsWith(prefix) && result.length() > prefix.length()) {
                    result = result.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return result;
    }

    private static Optional<ParsedResource> parsed(ResourceKind kind, String material) {
        String normalized = normalizeMaterial(material);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ParsedResource(kind, normalized));
    }

    public static String normalizeMaterial(String material) {
        String normalized = material == null ? "" : material.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.contains("/") || normalized.contains(":")
                || normalized.startsWith("_") || normalized.endsWith("_")) {
            return "";
        }
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9') && c != '_') {
                return "";
            }
        }
        return normalized;
    }

    /**
     * Strip common qualifiers/suffixes so {@code aether_metal} and {@code aether_ore} can be
     * associated with the same material family.  This is used only for linking non-element ingots
     * to their ore/raw/block forms, never for replacing the actual material name in tags.
     */
    public static String baseMaterial(String material) {
        String result = normalizeMaterial(material);
        if (result.isEmpty()) {
            return "";
        }
        boolean changed;
        do {
            changed = false;
            for (String prefix : BASE_PREFIXES) {
                if (result.startsWith(prefix) && result.length() > prefix.length()) {
                    result = result.substring(prefix.length());
                    changed = true;
                    break;
                }
            }
            if (changed) {
                continue;
            }
            for (String suffix : BASE_SUFFIXES) {
                if (result.endsWith(suffix) && result.length() > suffix.length()) {
                    result = result.substring(0, result.length() - suffix.length());
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return normalizeMaterial(result);
    }

    /**
     * Storage-block-only parser.  It is intentionally not used by the tag generator (to avoid
     * polluting c:storage_blocks with note_block/command_block), but the runtime index uses it to
     * recognise whitelisted blocks such as silver_block even when the mod does not provide a tag.
     */
    public static Optional<ParsedResource> parseStorageBlock(ResourceLocation id) {
        String path = id.getPath();
        if (path.indexOf('/') >= 0) {
            return Optional.empty();
        }
        if (path.startsWith("block_of_")) {
            return parsed(ResourceKind.STORAGE_BLOCK, path.substring("block_of_".length()));
        }
        if (path.startsWith("block_raw_")) {
            return parsed(ResourceKind.STORAGE_BLOCK, "raw_" + path.substring("block_raw_".length()));
        }
        if (path.startsWith("block_")) {
            return parsed(ResourceKind.STORAGE_BLOCK, path.substring("block_".length()));
        }
        if (path.endsWith("_block")) {
            return parsed(ResourceKind.STORAGE_BLOCK, path.substring(0, path.length() - "_block".length()));
        }
        return Optional.empty();
    }

    public record ParsedResource(ResourceKind kind, String material) {
    }
}
