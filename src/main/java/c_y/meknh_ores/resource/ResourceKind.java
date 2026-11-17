package c_y.meknh_ores.resource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * The conventional ({@code c:}) resource families handled by the mod.
 */
public enum ResourceKind {
    ORE("ores"),
    RAW_MATERIAL("raw_materials"),
    INGOT("ingots"),
    NUGGET("nuggets"),
    DUST("dusts"),
    STORAGE_BLOCK("storage_blocks");

    private static final String CONVENTION_NAMESPACE = "c";

    private final String folder;

    ResourceKind(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }

    public TagKey<Item> itemTag() {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(CONVENTION_NAMESPACE, folder));
    }

    public TagKey<Item> itemTag(String material) {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CONVENTION_NAMESPACE, folder + "/" + material));
    }

    public TagKey<Block> blockTag() {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(CONVENTION_NAMESPACE, folder));
    }

    public TagKey<Block> blockTag(String material) {
        return TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(CONVENTION_NAMESPACE, folder + "/" + material));
    }

    /** Match a {@code c:<folder>} or {@code c:<folder>/<material>} tag. */
    public static Optional<TagMatch> fromTag(ResourceLocation tagId) {
        if (!CONVENTION_NAMESPACE.equals(tagId.getNamespace())) {
            return Optional.empty();
        }
        String path = tagId.getPath();
        for (ResourceKind kind : values()) {
            if (path.equals(kind.folder)) {
                return Optional.of(new TagMatch(kind, ""));
            }
            String prefix = kind.folder + "/";
            if (path.startsWith(prefix) && path.length() > prefix.length()) {
                return Optional.of(new TagMatch(kind, path.substring(prefix.length())));
            }
        }
        return Optional.empty();
    }

    public record TagMatch(ResourceKind kind, String material) {
    }
}
