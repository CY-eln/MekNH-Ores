package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.resource.ResourceNaming;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A GregTech-style vein definition registered from KubeJS.
 *
 * <p>A vein belongs to exactly one {@link WorldGenLayer}; the layer decides which dimensions
 * are allowed and which blocks may be replaced.  Every {@code grid × grid} chunk cell selects
 * at most one vein per applicable layer, weighted by {@link #weight()}.</p>
 */
public record VeinDefinition(ResourceLocation id,
                             int weight,
                             int minClusterSize,
                             int maxClusterSize,
                             float density,
                             WorldGenLayer layer,
                             Set<ResourceLocation> dimensions,
                             List<String> biomes,
                             List<BiomeWeight> biomeWeights,
                             int minY,
                             int maxY,
                             HeightRangePlacement heightRange,
                             float discardChanceOnAirExposure,
                             List<WeightedOre> primary,
                             List<WeightedOre> secondary,
                             List<WeightedOre> sporadic,
                             VeinGenerator generator,
                             List<IndicatorDefinition> indicators) {

    public VeinDefinition {
        dimensions = Set.copyOf(dimensions);
        biomes = List.copyOf(biomes);
        biomeWeights = List.copyOf(biomeWeights);
        primary = List.copyOf(primary);
        secondary = List.copyOf(secondary);
        sporadic = List.copyOf(sporadic);
        indicators = List.copyOf(indicators);
        if (minClusterSize > maxClusterSize) {
            int swap = minClusterSize;
            minClusterSize = maxClusterSize;
            maxClusterSize = swap;
        }
        minClusterSize = Math.max(1, minClusterSize);
        maxClusterSize = Math.max(1, maxClusterSize);
        density = Math.max(0f, Math.min(1f, density));
        discardChanceOnAirExposure = Math.max(0f, Math.min(1f, discardChanceOnAirExposure));
        if (minY > maxY) {
            int swap = minY;
            minY = maxY;
            maxY = swap;
        }
    }

    public int clusterSize(RandomSource random) {
        if (maxClusterSize <= minClusterSize) {
            return minClusterSize;
        }
        return minClusterSize + random.nextInt(maxClusterSize - minClusterSize + 1);
    }

    public List<WeightedOre> allOres() {
        if (generator != null) {
            return generator.allOres();
        }
        List<WeightedOre> result = new ArrayList<>(primary.size() + secondary.size() + sporadic.size());
        result.addAll(primary);
        result.addAll(secondary);
        result.addAll(sporadic);
        return List.copyOf(result);
    }

    /** Material family of the primary ore, used by pack helpers and diagnostics. */
    public String family() {
        List<WeightedOre> ores = allOres();
        if (ores.isEmpty()) {
            return "";
        }
        return ResourceNaming.parse(ores.get(0).id())
                .map(ResourceNaming.ParsedResource::material)
                .orElse(ores.get(0).id().getPath());
    }

    public boolean matchesDimension(ResourceLocation dimension) {
        return layer != null
                && layer.appliesTo(dimension)
                && (dimensions.isEmpty() || dimensions.contains(dimension));
    }

    public boolean matchesBiome(Holder<Biome> biome) {
        if (biomes.isEmpty()) {
            return true;
        }
        for (String filter : biomes) {
            if (matchesBiomeFilter(filter, biome)) {
                return true;
            }
        }
        return false;
    }

    public int weightForBiome(Holder<Biome> biome) {
        int result = weight;
        for (BiomeWeight modifier : biomeWeights) {
            if (modifier.matches(biome)) {
                result += modifier.addedWeight();
            }
        }
        return result;
    }

    /**
     * Samples the vein center Y from the definition's height range.  Absolute anchors are used,
     * so this works both for the origin chunk and for neighboring chunks consuming the vein.
     */
    public BlockPos sampleCenter(WorldGenLevel level, ChunkGenerator generator,
                                 RandomSource random, BlockPos base) {
        return heightRange.getPositions(
                        new PlacementContext(level, generator, Optional.empty()),
                        random, base)
                .findFirst()
                .orElse(null);
    }

    public static boolean matchesBiomeFilter(String filter, Holder<Biome> biome) {
        if (filter == null || filter.isBlank()) {
            return false;
        }
        if (filter.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(filter.substring(1));
            return tagId != null && biome.is(TagKey.create(Registries.BIOME, tagId));
        }
        ResourceLocation biomeId = ResourceLocation.tryParse(filter);
        return biomeId != null && biome.unwrapKey()
                .map(key -> key.location().equals(biomeId))
                .orElse(false);
    }
}
