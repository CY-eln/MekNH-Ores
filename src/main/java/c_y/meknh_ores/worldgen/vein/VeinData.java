package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.OresConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Runtime registry of KubeJS-defined ore veins, plus the per-level vein cache used to make
 * veins span chunk borders without regenerating them for every chunk.
 */
public final class VeinData {

    private static final Map<ResourceLocation, VeinDefinition> VEINS = new LinkedHashMap<>();
    private static final Set<ResourceLocation> TAKEN_OVER_BLOCKS = new LinkedHashSet<>();
    private static final Map<ServerLevel, LevelCache> CACHE = new WeakHashMap<>();

    private VeinData() {
    }

    public static synchronized void register(VeinDefinition definition) {
        VEINS.put(definition.id(), definition);
        TAKEN_OVER_BLOCKS.clear();
        for (VeinDefinition vein : VEINS.values()) {
            for (WeightedOre ore : vein.allOres()) {
                TAKEN_OVER_BLOCKS.add(ore.id());
            }
        }
        clearCache();
    }

    public static synchronized boolean hasVeins() {
        return !VEINS.isEmpty();
    }

    public static synchronized boolean hasVeinsForDimension(ResourceLocation dimension) {
        for (VeinDefinition vein : VEINS.values()) {
            if (vein.matchesDimension(dimension)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean isTakenOverBlock(ResourceLocation blockId) {
        return TAKEN_OVER_BLOCKS.contains(blockId);
    }

    /** Immutable copy used by the ore-feature remover and diagnostics. */
    public static synchronized Set<ResourceLocation> takenOverBlocks() {
        return Set.copyOf(TAKEN_OVER_BLOCKS);
    }

    public static synchronized List<VeinDefinition> all() {
        return List.copyOf(VEINS.values());
    }

    public static synchronized void clearCache() {
        CACHE.clear();
    }

    /**
     * All veins that can contribute blocks to {@code chunk}.  Origin chunks are grid nodes; the
     * search radius is derived from the largest vein plus the configured random offset.
     */
    public static List<GeneratedVein> veinsForChunk(WorldGenLevel level, ChunkGenerator generator,
                                                    ChunkPos chunk) {
        if (!OresConfig.enableVeinTakeover() || !hasVeins()) {
            return List.of();
        }

        int grid = OresConfig.veinGridSize();
        int distance = searchDistance();
        int startX = Math.floorDiv(chunk.x - distance, grid) * grid;
        int startZ = Math.floorDiv(chunk.z - distance, grid) * grid;
        int endX = chunk.x + distance;
        int endZ = chunk.z + distance;

        List<GeneratedVein> result = new ArrayList<>();
        for (int originX = startX; originX <= endX; originX += grid) {
            for (int originZ = startZ; originZ <= endZ; originZ += grid) {
                result.addAll(veinsForOrigin(level, generator, new ChunkPos(originX, originZ)));
            }
        }
        return result;
    }

    private static List<GeneratedVein> veinsForOrigin(WorldGenLevel level, ChunkGenerator generator,
                                                      ChunkPos origin) {
        ServerLevel serverLevel = level.getLevel();
        ResourceLocation dimension = serverLevel.dimension().location();

        synchronized (VeinData.class) {
            LevelCache cache = CACHE.computeIfAbsent(serverLevel, ignored -> new LevelCache());
            List<GeneratedVein> cached = cache.veins.get(origin);
            if (cached != null) {
                return cached;
            }

            List<GeneratedVein> generated = new ArrayList<>();
            long seed = serverLevel.getSeed();
            for (WorldGenLayer layer : WorldGenLayers.all()) {
                if (!layer.appliesTo(dimension)) {
                    continue;
                }
                long layerSeed = seed
                        ^ origin.toLong()
                        ^ ((long) layer.id().hashCode() * 0x9E3779B97F4A7C15L);
                RandomSource random = new XoroshiroRandomSource(layerSeed);
                GeneratedVein vein = generateForLayer(level, generator, origin, layer, random);
                if (vein != null) {
                    generated.add(vein);
                }
            }

            List<GeneratedVein> result = List.copyOf(generated);
            cache.veins.put(origin, result);
            return result;
        }
    }

    private static GeneratedVein generateForLayer(WorldGenLevel level, ChunkGenerator generator,
                                                  ChunkPos origin, WorldGenLayer layer,
                                                  RandomSource random) {
        int offset = OresConfig.veinRandomOffset();
        BlockPos base = origin.getMiddleBlockPosition(0);
        if (offset > 0) {
            base = base.offset(
                    random.nextInt(offset * 2 + 1) - offset,
                    0,
                    random.nextInt(offset * 2 + 1) - offset);
        }

        Holder<Biome> biome = biomeAt(level, base);
        ResourceLocation dimension = level.getLevel().dimension().location();

        List<VeinDefinition> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int totalWeight = 0;
        for (VeinDefinition vein : all()) {
            if (vein.layer() != layer
                    || !vein.matchesDimension(dimension)
                    || !vein.matchesBiome(biome)) {
                continue;
            }
            int weight = vein.weightForBiome(biome);
            if (weight <= 0) {
                continue;
            }
            candidates.add(vein);
            weights.add(weight);
            totalWeight += weight;
        }
        if (candidates.isEmpty() || totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        VeinDefinition selected = candidates.get(candidates.size() - 1);
        for (int i = 0; i < candidates.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                selected = candidates.get(i);
                break;
            }
        }

        BlockPos center = selected.sampleCenter(level, generator, random, base);
        if (center == null) {
            return null;
        }
        center = center.atY(Math.max(level.getMinBuildHeight(),
                Math.min(center.getY(), level.getMaxBuildHeight() - 1)));
        if (selected.generator() == null) {
            return null;
        }

        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> blocks =
                selected.generator().generate(level, random, selected, center);

        if (blocks.isEmpty()) {
            MekNHOres.LOGGER.debug("Vein {} at {} produced no blocks", selected.id(), center);
        }

        List<IndicatorDefinition> indicatorDefinitions = selected.indicators();
        List<GeneratedVein.IndicatorPatch> patches = new ArrayList<>();
        if (OresConfig.veinIndicators()) {
            for (IndicatorDefinition indicator : indicatorDefinitions) {
                patches.add(new GeneratedVein.IndicatorPatch(
                        indicator, indicator.candidates(center, random)));
            }
        }

        return new GeneratedVein(origin, selected, blocks, patches);
    }

    /**
     * Samples the biome directly from the chunk generator's biome source.  Calling
     * {@code level.getBiome(...)} for a neighboring origin chunk throws in a WorldGenRegion
     * because that chunk is not loaded, so this avoids a cascading chunk lookup.
     */
    private static Holder<Biome> biomeAt(WorldGenLevel level, BlockPos pos) {
        ServerLevel server = level.getLevel();
        Climate.Sampler sampler = server.getChunkSource().randomState().sampler();
        return server.getChunkSource().getGenerator().getBiomeSource()
                .getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, sampler);
    }

    private static int searchDistance() {
        int largest = 0;
        int indicatorRadius = 0;
        for (VeinDefinition vein : all()) {
            largest = Math.max(largest, vein.maxClusterSize());
            for (IndicatorDefinition indicator : vein.indicators()) {
                indicatorRadius = Math.max(indicatorRadius, indicator.radius());
            }
        }
        double half = largest / 2.0;
        return Math.max(1, (int) Math.ceil(
                (half + OresConfig.veinRandomOffset() + indicatorRadius) / 16.0));
    }

    private static final class LevelCache {
        private final Map<ChunkPos, List<GeneratedVein>> veins =
                new LinkedHashMap<>(64, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<ChunkPos, List<GeneratedVein>> eldest) {
                        return size() > OresConfig.veinCacheSize();
                    }
                };
    }
}
