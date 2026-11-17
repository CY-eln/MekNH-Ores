package c_y.meknh_ores.resource;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.OresConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Runtime view of all recognised resource families, built from item registry names and the loaded
 * item tags.  The generated tag pack makes the tag side complete; the name parser is a fallback for
 * packs that disable tag generation.
 */
public final class UnifiedResourceIndex {
    private static volatile UnifiedResourceIndex instance;

    private final Map<ResourceLocation, Set<Variant>> variantsByItem;
    private final Map<ResourceKey, UnifiedResource> resources;
    private final Set<String> exemptNamespaces;
    private final Set<String> cleanupMaterials;

    private UnifiedResourceIndex(Map<ResourceLocation, Set<Variant>> variantsByItem,
                                 Map<ResourceKey, UnifiedResource> resources,
                                 Set<String> exemptNamespaces,
                                 Set<String> cleanupMaterials) {
        this.variantsByItem = variantsByItem;
        this.resources = resources;
        this.exemptNamespaces = exemptNamespaces;
        this.cleanupMaterials = cleanupMaterials;
    }

    public static UnifiedResourceIndex get() {
        UnifiedResourceIndex local = instance;
        if (local == null) {
            synchronized (UnifiedResourceIndex.class) {
                local = instance;
                if (local == null) {
                    local = build();
                    instance = local;
                }
            }
        }
        return local;
    }

    public static void invalidate() {
        instance = null;
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        invalidate();
    }

    public boolean isForeign(ResourceLocation id) {
        return !exemptNamespaces.contains(id.getNamespace()) && !OresConfig.isExemptItem(id);
    }

    private boolean isCleanupVariant(Variant variant) {
        if (!OresConfig.isCleanupResourceKind(variant.kind())) {
            return false;
        }
        return OresConfig.isCleanupMaterial(variant.kind(), variant.material());
    }

    public boolean isCleanup(ResourceKind kind, String material) {
        return isCleanupVariant(new Variant(kind, material));
    }

    /** Used by world generation to decide whether an ore/raw block belongs to the cleanup whitelist. */
    public boolean isCleanupBlock(ResourceLocation blockId) {
        // Explicit ids/tags/namespaces always win over the namespace exemption.
        if (OresConfig.isExplicitCleanupBlock(blockId)) {
            return true;
        }
        if (OresConfig.isExemptNamespace(blockId.getNamespace())) {
            return false;
        }
        if (ResourceNaming.parse(blockId)
                .map(parsed -> isCleanupVariant(new Variant(parsed.kind(), parsed.material())))
                .orElse(false)) {
            return true;
        }

        // Some mods use unusual ids (for example occultism:silver_ore_deepslate).  Their block
        // tags still point at the right material family, so use c:ores/<material> as a fallback.
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        for (TagKey<Block> tag : BuiltInRegistries.BLOCK.wrapAsHolder(block).tags().toList()) {
            Optional<ResourceKind.TagMatch> match = ResourceKind.fromTag(tag.location());
            if (match.isPresent() && !match.get().material().isEmpty()
                    && OresConfig.isCleanupMaterial(match.get().kind(), match.get().material())) {
                return true;
            }
        }
        return false;
    }

    /** Fallback for custom ore features whose config cannot be inspected. */
    public boolean isCleanupFeature(ResourceLocation featureId) {
        if (ResourceNaming.parse(featureId)
                .map(parsed -> isCleanupVariant(new Variant(parsed.kind(), parsed.material())))
                .orElse(false)) {
            return true;
        }
        String path = featureId.getPath().toLowerCase(Locale.ROOT);
        for (String material : cleanupMaterials) {
            if (path.equals(material)
                    || path.startsWith(material + "_")
                    || path.endsWith("_" + material)
                    || path.contains("_" + material + "_")) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldHide(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // Explicit ids/tags/namespaces win over namespace exemptions, but exempt_items still protects.
        if (OresConfig.isExplicitlyExemptItem(id)) {
            return false;
        }
        if (OresConfig.isExplicitCleanupItem(id)) {
            return true;
        }
        if (!isForeign(id)) {
            return false;
        }
        Set<Variant> variants = variantsByItem.get(id);
        if (variants == null || variants.isEmpty()) {
            return false;
        }
        for (Variant variant : variants) {
            if (!isCleanupVariant(variant)) {
                continue;
            }
            if (!OresConfig.hide(variant.kind())
                    && !OresConfig.isDataCleanupMaterial(variant.kind(), variant.material())) {
                continue;
            }
            if (OresConfig.hideOnlyIfCanonicalExists()) {
                UnifiedResource resource = resources.get(new ResourceKey(variant.kind(), variant.material()));
                if (resource == null || !resource.canonicalExempt()) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    public Optional<ItemStack> canonical(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Set<Variant> variants = variantsByItem.get(id);
        if (variants == null || variants.isEmpty()) {
            return Optional.empty();
        }
        for (Variant variant : variants) {
            if (!isCleanupVariant(variant)) {
                continue;
            }
            UnifiedResource resource = resources.get(new ResourceKey(variant.kind(), variant.material()));
            if (resource != null) {
                Item item = BuiltInRegistries.ITEM.get(resource.canonical());
                if (item != null) {
                    return Optional.of(new ItemStack(item));
                }
            }
        }
        return Optional.of(new ItemStack(stack.getItem()));
    }

    public List<ItemStack> variants(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Set<Variant> variants = variantsByItem.get(id);
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        Variant cleanup = null;
        for (Variant variant : variants) {
            if (isCleanupVariant(variant)) {
                cleanup = variant;
                break;
            }
        }
        if (cleanup == null) {
            return List.of(new ItemStack(stack.getItem()));
        }
        UnifiedResource resource = resources.get(new ResourceKey(cleanup.kind(), cleanup.material()));
        if (resource == null) {
            return List.of(new ItemStack(stack.getItem()));
        }
        List<ItemStack> result = new ArrayList<>(resource.variants().size());
        for (ResourceLocation variantId : resource.variants()) {
            Item item = BuiltInRegistries.ITEM.get(variantId);
            if (item != null) {
                result.add(new ItemStack(item));
            }
        }
        return List.copyOf(result);
    }

    public boolean isRecognised(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return variantsByItem.containsKey(id);
    }

    public Optional<UnifiedResource> resource(ResourceKind kind, String material) {
        return Optional.ofNullable(resources.get(new ResourceKey(kind, material)));
    }

    public int familyCount() {
        return resources.size();
    }

    public int recognisedItemCount() {
        return variantsByItem.size();
    }

    private static UnifiedResourceIndex build() {
        Set<String> exemptNamespaces = OresConfig.exemptNamespaces();
        Map<ResourceLocation, Set<Variant>> variantsByItem = new LinkedHashMap<>();
        Map<ResourceKey, Set<ResourceLocation>> grouped = new LinkedHashMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

            ResourceNaming.parse(id).ifPresent(parsed -> {
                Variant variant = new Variant(parsed.kind(), parsed.material());
                variantsByItem.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(variant);
                grouped.computeIfAbsent(new ResourceKey(parsed.kind(), parsed.material()),
                        ignored -> new LinkedHashSet<>()).add(id);
            });

            // Storage blocks are not added to the generated tags (to avoid note_block etc.), but a
            // whitelisted silver_block still needs to be recognised for JEI hiding.
            ResourceNaming.parseStorageBlock(id).ifPresent(parsed -> {
                if (!OresConfig.isCleanupMaterial(ResourceKind.STORAGE_BLOCK, parsed.material())) {
                    return;
                }
                Variant variant = new Variant(ResourceKind.STORAGE_BLOCK, parsed.material());
                variantsByItem.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(variant);
                grouped.computeIfAbsent(new ResourceKey(ResourceKind.STORAGE_BLOCK, parsed.material()),
                        ignored -> new LinkedHashSet<>()).add(id);
            });

            for (TagKey<Item> tag : BuiltInRegistries.ITEM.wrapAsHolder(item).tags().toList()) {
                ResourceKind.fromTag(tag.location()).ifPresent(match -> {
                    if (match.material().isEmpty()) {
                        return;
                    }
                    String material = ResourceNaming.normalizeMaterial(match.material());
                    if (material.isEmpty()) {
                        return;
                    }
                    Variant variant = new Variant(match.kind(), material);
                    variantsByItem.computeIfAbsent(id, ignored -> new LinkedHashSet<>()).add(variant);
                    grouped.computeIfAbsent(new ResourceKey(match.kind(), material),
                            ignored -> new LinkedHashSet<>()).add(id);
                });
            }
        }

        Map<ResourceKey, UnifiedResource> resources = new LinkedHashMap<>();
        for (Map.Entry<ResourceKey, Set<ResourceLocation>> entry : grouped.entrySet()) {
            ResourceKey key = entry.getKey();
            List<ResourceLocation> sorted = entry.getValue().stream()
                    .sorted(canonicalComparator(key.kind()))
                    .toList();
            if (sorted.isEmpty()) {
                continue;
            }

            ResourceLocation canonical = sorted.get(0);
            boolean canonicalExempt = exemptNamespaces.contains(canonical.getNamespace())
                    || OresConfig.isExemptItem(canonical);
            for (ResourceLocation candidate : sorted) {
                if (exemptNamespaces.contains(candidate.getNamespace()) || OresConfig.isExemptItem(candidate)) {
                    canonical = candidate;
                    canonicalExempt = true;
                    break;
                }
            }

            resources.put(key, new UnifiedResource(key.kind(), key.material(), canonical,
                    List.copyOf(sorted), canonicalExempt));
        }

        // Whitelisted material names (plus base forms) used by the feature-id fallback.
        Set<String> cleanupMaterials = new LinkedHashSet<>(OresConfig.cleanupMaterials());

        MekNHOres.LOGGER.debug("Unified {} resource families across {} items; {} cleanup material(s)",
                resources.size(), variantsByItem.size(), cleanupMaterials.size());
        return new UnifiedResourceIndex(Map.copyOf(variantsByItem), Map.copyOf(resources),
                Set.copyOf(exemptNamespaces), Set.copyOf(cleanupMaterials));
    }

    private static Comparator<ResourceLocation> canonicalComparator(ResourceKind kind) {
        return Comparator
                .comparingInt((ResourceLocation id) -> OresConfig.canonicalPriority(id.getNamespace()))
                .thenComparingInt(id -> kind == ResourceKind.ORE && id.getPath().contains("deepslate") ? 1 : 0)
                .thenComparingInt(id -> id.getPath().length())
                .thenComparing(ResourceLocation::toString);
    }

    public record Variant(ResourceKind kind, String material) {
    }

    public record ResourceKey(ResourceKind kind, String material) {
    }

    public record UnifiedResource(ResourceKind kind, String material, ResourceLocation canonical,
                                  List<ResourceLocation> variants, boolean canonicalExempt) {
    }
}
