package c_y.meknh_ores.worldgen.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shape algorithms.  They are deliberately kept free of GT classes so MekNH Ores can run
 * without GregTech installed.
 */
final class VeinGeneratorEngine {

    private VeinGeneratorEngine() {
    }

    static Map<BlockPos, BlockState> legacy(WorldGenLevel level, RandomSource random,
                                            VeinDefinition vein, BlockPos origin,
                                            VeinGenerator.Legacy generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        int size = vein.clusterSize(random);
        int radius = Math.max(1, size / 2);
        int yRadius = Math.max(1, Math.min(radius, size / 6));
        double xzRadiusSq = (double) radius * radius;
        double yRadiusSq = (double) yRadius * yRadius;

        for (int x = -radius; x <= radius; x++) {
            if ((double) x * x > xzRadiusSq) {
                continue;
            }
            for (int z = -radius; z <= radius; z++) {
                if ((double) x * x + (double) z * z > xzRadiusSq) {
                    continue;
                }
                for (int y = -yRadius; y <= yRadius; y++) {
                    double normalized = ((double) x * x + (double) z * z) / xzRadiusSq
                            + (double) y * y / yRadiusSq;
                    if (normalized > 1.0) {
                        continue;
                    }
                    if (random.nextFloat() > vein.density()) {
                        continue;
                    }
                    WeightedOre ore = legacyPick(generator, random);
                    put(result, level, origin.offset(x, y, z), ore);
                }
            }
        }
        return result;
    }

    private static WeightedOre legacyPick(VeinGenerator.Legacy generator, RandomSource random) {
        double roll = random.nextDouble();
        if (roll < 0.08 && !generator.sporadic().isEmpty()) {
            return pick(generator.sporadic(), random);
        }
        if (roll < 0.38 && !generator.secondary().isEmpty()) {
            return pick(generator.secondary(), random);
        }
        if (!generator.primary().isEmpty()) {
            return pick(generator.primary(), random);
        }
        if (!generator.secondary().isEmpty()) {
            return pick(generator.secondary(), random);
        }
        return pick(generator.sporadic(), random);
    }

    static Map<BlockPos, BlockState> layered(WorldGenLevel level, RandomSource random,
                                             VeinDefinition vein, BlockPos origin,
                                             VeinGenerator.Layered generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        List<VeinGenerator.LayerPattern> patterns = generator.patterns();
        if (patterns.isEmpty()) {
            return result;
        }

        VeinGenerator.LayerPattern pattern = patterns.size() == 1
                ? patterns.get(0)
                : patterns.get(random.nextInt(patterns.size()));

        int size = vein.clusterSize(random);
        int radius = Mth.ceil(size / 2f);
        int width = radius * 2 + 1;
        int height = radius * 2 + 1;
        int length = radius * 2 + 1;
        int xMin = origin.getX() - radius;
        int yMin = origin.getY() - radius;
        int zMin = origin.getZ() - radius;

        List<VeinGenerator.Layer> resolvedLayers = new ArrayList<>();
        List<Float> layerDiameterOffsets = new ArrayList<>();

        int layerCoordinate = random.nextInt(4);
        int slantyCoordinate = random.nextInt(3);
        float slope = random.nextFloat() * 0.75f;

        for (int xOffset = 0; xOffset < width; xOffset++) {
            float sizeFractionX = xOffset * 2f / width - 1;
            float xSizeSqr = sizeFractionX * sizeFractionX;
            if (xSizeSqr > 1) {
                continue;
            }

            for (int yOffset = 0; yOffset < height; yOffset++) {
                float sizeFractionY = yOffset * 2f / height - 1;
                float ySizeSqr = sizeFractionY * sizeFractionY;
                if (xSizeSqr + ySizeSqr > 1) {
                    continue;
                }
                if (level.isOutsideBuildHeight(yMin + yOffset)) {
                    continue;
                }

                for (int zOffset = 0; zOffset < length; zOffset++) {
                    float sizeFractionZ = zOffset * 2f / length - 1;
                    float zSizeSqr = sizeFractionZ * sizeFractionZ;
                    if (xSizeSqr + ySizeSqr + zSizeSqr > 1) {
                        continue;
                    }

                    int layerIndex = layerCoordinate == 0 ? zOffset
                            : layerCoordinate == 1 ? xOffset : yOffset;
                    if (slantyCoordinate != layerCoordinate) {
                        int slantyOffset = slantyCoordinate == 0 ? zOffset
                                : slantyCoordinate == 1 ? xOffset : yOffset;
                        layerIndex += (int) (Mth.floor(slantyOffset) * slope);
                    }

                    while (layerIndex >= resolvedLayers.size()) {
                        VeinGenerator.Layer previous = resolvedLayers.isEmpty()
                                ? null : resolvedLayers.get(resolvedLayers.size() - 1);
                        VeinGenerator.Layer next = pattern.rollNext(previous, random);
                        float offset = random.nextFloat() * 0.5f + 0.5f;

                        if (next == null) {
                            if (resolvedLayers.isEmpty()) {
                                break;
                            }
                            resolvedLayers.add(previous);
                            layerDiameterOffsets.add(offset);
                            continue;
                        }

                        int count = next.minSize() + random.nextInt(1 + next.maxSize() - next.minSize());
                        for (int i = 0; i < count; i++) {
                            resolvedLayers.add(next);
                            layerDiameterOffsets.add(offset);
                        }
                    }

                    if (resolvedLayers.isEmpty() || layerIndex >= resolvedLayers.size()) {
                        continue;
                    }
                    if (xSizeSqr + ySizeSqr + zSizeSqr > layerDiameterOffsets.get(layerIndex)) {
                        continue;
                    }

                    VeinGenerator.Layer layer = resolvedLayers.get(layerIndex);
                    WeightedOre ore = layer.pick(random);
                    put(result, level, new BlockPos(xMin + xOffset, yMin + yOffset, zMin + zOffset), ore);
                }
            }
        }
        return result;
    }

    static Map<BlockPos, BlockState> classic(WorldGenLevel level, RandomSource random,
                                             VeinDefinition vein, BlockPos origin,
                                             VeinGenerator.Classic generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        int radius = Math.max(1, vein.clusterSize(random) / 2);
        int ySize = Math.max(1, radius / 2);
        int yMax = Math.min(Math.max(ySize, radius), Math.max(1, generator.yRadius()));

        int max = Math.max(ySize, radius);
        double yz2 = (double) ySize * ySize * radius * radius;
        double xz2 = (double) radius * radius * radius * radius;
        double xy2 = (double) radius * radius * ySize * ySize;
        double xyz2 = xy2 * radius * radius;

        int xPos = origin.getX();
        int yPos = origin.getY();
        int zPos = origin.getZ();
        BlockPos minPos = new BlockPos(xPos - max, yPos - yMax, zPos - max);

        int primarySize = Math.max(1, generator.primary().minSize());
        int secondarySize = Math.max(1, generator.secondary().minSize());
        int betweenSize = Math.max(1, generator.between().minSize());
        int sporadicDivisor = Math.max(1, primarySize + secondarySize - 1);
        int startPrimary = secondarySize;
        int startBetween = secondarySize - betweenSize / 2;

        for (int xOffset = -max; xOffset <= max; xOffset++) {
            double xr = yz2 * xOffset * xOffset;
            if (xr > xyz2) {
                continue;
            }
            for (int yOffset = -yMax; yOffset <= yMax; yOffset++) {
                double yr = xr + xz2 * yOffset * yOffset + xy2;
                if (yr > xyz2) {
                    continue;
                }
                if (level.isOutsideBuildHeight(yOffset + yPos)) {
                    continue;
                }
                for (int zOffset = -max; zOffset <= max; zOffset++) {
                    double zr = yr + xy2 * zOffset * zOffset;
                    if (zr > xyz2) {
                        continue;
                    }

                    BlockPos pos = new BlockPos(xOffset + xPos, yOffset + yPos, zOffset + zPos);
                    int layer = pos.getY() - minPos.getY();

                    if (layer >= startBetween && layer - startBetween + 1 <= betweenSize) {
                        if (random.nextFloat() <= vein.density() / 2f) {
                            put(result, level, pos, generator.between().pick(random));
                            continue;
                        }
                    }

                    if (layer >= startPrimary) {
                        if (random.nextFloat() <= vein.density()) {
                            put(result, level, pos, generator.primary().pick(random));
                            continue;
                        }
                    } else {
                        if (random.nextFloat() <= vein.density()) {
                            put(result, level, pos, generator.secondary().pick(random));
                            continue;
                        }
                    }

                    if (random.nextFloat() <= vein.density() / sporadicDivisor) {
                        put(result, level, pos, generator.sporadic().pick(random));
                    }
                }
            }
        }
        return result;
    }

    static Map<BlockPos, BlockState> cuboid(WorldGenLevel level, RandomSource random,
                                            VeinDefinition vein, BlockPos origin,
                                            VeinGenerator.Cuboid generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        int size = Math.max(1, vein.clusterSize(random) / 2);
        int westBound = origin.getX() - random.nextInt(size);
        int eastBound = origin.getX() + 16 + random.nextInt(size);
        int northBound = origin.getZ() - random.nextInt(size);
        int southBound = origin.getZ() + 16 + random.nextInt(size);

        int topSize = Math.max(1, generator.top().minSize());
        int middleSize = Math.max(1, generator.middle().minSize());
        int bottomSize = Math.max(1, generator.bottom().minSize());
        int totalLayers = topSize + middleSize + bottomSize;
        int bottomLayer = (int) (totalLayers / 7.0f * 2.0f);
        int middleLayer1 = (int) (totalLayers / 7.0f * 3.0f);
        int middleLayer2 = (int) (totalLayers / 7.0f * 4.0f);
        int topLayer = (int) (totalLayers / 7.0f * 6.0f);
        int spreadDivisor = Math.max(1, (topSize + middleSize - 1) / 2);

        int minY = Math.max(vein.minY(), level.getMinBuildHeight());
        int maxY = Math.min(vein.maxY(), level.getMaxBuildHeight() - 1);
        int span = Math.max(1, maxY - minY - 5);
        int startY = minY + random.nextInt(span) - 1;

        for (int layerOffset = 0; layerOffset <= totalLayers; layerOffset++) {
            int layer = startY + layerOffset;
            if (level.isOutsideBuildHeight(layer)) {
                continue;
            }
            for (int x = westBound; x < eastBound; x++) {
                for (int z = northBound; z < southBound; z++) {
                    int xLength = x - origin.getX();
                    int zLength = z - origin.getZ();
                    double volume = Math.sqrt(2 + (xLength * xLength) + (zLength * zLength));
                    int localDensity = (int) Math.max(1, 8 * vein.density() / volume);
                    int weightX = Math.max(1, Math.max(
                            Math.abs(westBound - x), Math.abs(eastBound - x)) / localDensity);
                    int weightZ = Math.max(1, Math.max(
                            Math.abs(southBound - z), Math.abs(northBound - z)) / localDensity);

                    BlockPos pos = new BlockPos(x, layer, z);
                    boolean placed = false;
                    if (layerOffset <= bottomLayer) {
                        placed = placeCuboid(result, level, pos, random, generator.bottom(), weightX, weightZ, 1f);
                        if (!placed) {
                            placed = placeCuboid(result, level, pos, random, generator.spread(), weightX, weightZ,
                                    vein.density() * spreadDivisor);
                        }
                    } else if (layerOffset <= middleLayer1) {
                        placed = placeCuboid(result, level, pos, random, generator.middle(), weightX, weightZ, 1f);
                        if (!placed) {
                            placed = placeCuboid(result, level, pos, random, generator.bottom(), weightX, weightZ, 1f);
                        }
                        if (!placed) {
                            placed = placeCuboid(result, level, pos, random, generator.spread(), weightX, weightZ,
                                    vein.density() * spreadDivisor);
                        }
                    } else if (layerOffset <= middleLayer2) {
                        placed = placeCuboid(result, level, pos, random, generator.middle(), weightX, weightZ, 1f);
                        if (!placed) {
                            placed = placeCuboid(result, level, pos, random, generator.spread(), weightX, weightZ,
                                    vein.density() * spreadDivisor);
                        }
                    } else if (layerOffset <= topLayer) {
                        placed = placeCuboid(result, level, pos, random, generator.middle(), weightX, weightZ, 1f);
                        if (!placed) {
                            placed = placeCuboid(result, level, pos, random, generator.top(), weightX, weightZ, 1f);
                        }
                        if (!placed) {
                            placed = placeCuboid(result, level, pos, random, generator.spread(), weightX, weightZ,
                                    vein.density() * spreadDivisor);
                        }
                    } else {
                        placed = placeCuboid(result, level, pos, random, generator.top(), weightX, weightZ, 1f);
                        if (!placed) {
                            placeCuboid(result, level, pos, random, generator.spread(), weightX, weightZ,
                                    vein.density() * spreadDivisor);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static boolean placeCuboid(Map<BlockPos, BlockState> result, WorldGenLevel level,
                                       BlockPos pos, RandomSource random, VeinGenerator.Layer layer,
                                       int weightX, int weightZ, float chance) {
        if (chance < 1f && random.nextFloat() > chance) {
            return false;
        }
        if (random.nextInt(weightX) != 0 && random.nextInt(weightZ) != 0) {
            return false;
        }
        WeightedOre ore = layer.pick(random);
        return put(result, level, pos, ore);
    }

    static Map<BlockPos, BlockState> dike(WorldGenLevel level, RandomSource random,
                                          VeinDefinition vein, BlockPos origin,
                                          VeinGenerator.Dike generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
        NormalNoise noise = NormalNoise.create(worldgenRandom, -2, 4.0D);

        int size = vein.clusterSize(random);
        int radius = Mth.ceil(size / 2f);
        int xPos = origin.getX() - 8 + random.nextInt(16);
        int zPos = origin.getZ() - 8 + random.nextInt(16);
        int minY = Math.max(vein.minY(), level.getMinBuildHeight());
        int maxY = Math.min(vein.maxY(), level.getMaxBuildHeight() - 1);

        for (int y = minY; y <= maxY; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * 2) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(xPos + dx, y, zPos + dz);
                    if (noise.getValue(dx, y, dz) < 0.5 || random.nextFloat() > vein.density()) {
                        continue;
                    }
                    List<VeinGenerator.DikeBlock> valid = new ArrayList<>();
                    for (VeinGenerator.DikeBlock block : generator.blocks()) {
                        if (y >= block.minY() && y <= block.maxY()) {
                            valid.add(block);
                        }
                    }
                    if (valid.isEmpty()) {
                        continue;
                    }
                    VeinGenerator.DikeBlock block = pickDike(valid, random);
                    put(result, level, pos, block == null ? null : block.ore());
                }
            }
        }
        return result;
    }

    private static VeinGenerator.DikeBlock pickDike(List<VeinGenerator.DikeBlock> blocks, RandomSource random) {
        int total = 0;
        for (VeinGenerator.DikeBlock block : blocks) {
            total += Math.max(1, block.ore().weight());
        }
        int roll = random.nextInt(total);
        for (VeinGenerator.DikeBlock block : blocks) {
            roll -= Math.max(1, block.ore().weight());
            if (roll < 0) {
                return block;
            }
        }
        return blocks.get(blocks.size() - 1);
    }

    static Map<BlockPos, BlockState> veined(WorldGenLevel level, RandomSource random,
                                             VeinDefinition vein, BlockPos origin,
                                             VeinGenerator.Veined generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();

        RandomState randomState = level.getLevel().getChunkSource().randomState();
        Registry<NormalNoise.NoiseParameters> noiseRegistry =
                level.registryAccess().registryOrThrow(Registries.NOISE);

        Holder<NormalNoise.NoiseParameters> oreVeininess =
                noiseRegistry.getHolderOrThrow(Noises.ORE_VEININESS);
        Holder<NormalNoise.NoiseParameters> oreVeinA =
                noiseRegistry.getHolderOrThrow(Noises.ORE_VEIN_A);
        Holder<NormalNoise.NoiseParameters> oreVeinB =
                noiseRegistry.getHolderOrThrow(Noises.ORE_VEIN_B);

        // Exact GTCEu density functions:
        // vein_toggle = interpolated(noise(ORE_VEININESS, 1.5, 1.5))
        // vein_ridged = -0.08 + max(|interpolated(noise(ORE_VEIN_A, 4, 4))|,
        //                            |interpolated(noise(ORE_VEIN_B, 4, 4))|)
        DensityFunction veinToggle = mapToNoise(
                DensityFunctions.interpolated(DensityFunctions.noise(oreVeininess, 1.5f, 1.5f)),
                randomState);
        DensityFunction veinRidged = mapToNoise(
                DensityFunctions.add(
                        DensityFunctions.constant(-0.08f),
                        DensityFunctions.max(
                                DensityFunctions.interpolated(
                                        DensityFunctions.noise(oreVeinA, 4.0f, 4.0f)).abs(),
                                DensityFunctions.interpolated(
                                        DensityFunctions.noise(oreVeinB, 4.0f, 4.0f)).abs())),
                randomState);

        Blender blender = level instanceof WorldGenRegion region
                ? Blender.of(region)
                : Blender.empty();

        int size = Math.max(1, vein.clusterSize(random));
        int radius = Mth.ceil(size / 2f);
        int minY = Math.max(vein.minY(), level.getMinBuildHeight());
        int maxY = Math.min(vein.maxY(), level.getMaxBuildHeight() - 1);

        int randOffsetX = random.nextInt(16);
        int randOffsetY = random.nextInt(16);
        int randOffsetZ = random.nextInt(16);

        // GT uses a bounding box, not a sphere.  The noise field itself determines the shape.
        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int y = origin.getY() - radius; y <= origin.getY() + radius; y++) {
                if (level.isOutsideBuildHeight(y)) {
                    continue;
                }
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    final int blockX = x;
                    final int blockY = y;
                    final int blockZ = z;
                    DensityFunction.FunctionContext context = new DensityFunction.FunctionContext() {
                        @Override
                        public int blockX() {
                            return blockX + randOffsetX;
                        }

                        @Override
                        public int blockY() {
                            return blockY + randOffsetY;
                        }

                        @Override
                        public int blockZ() {
                            return blockZ + randOffsetZ;
                        }

                        @Override
                        public Blender getBlender() {
                            return blender;
                        }
                    };

                    double toggleNoise = veinToggle.compute(context);
                    int distanceToMin = origin.getY() - minY;
                    int distanceToMax = maxY - origin.getY();
                    if (distanceToMin < 0 || distanceToMax < 0) {
                        continue;
                    }
                    int lowY = Math.min(distanceToMax, distanceToMin);
                    double edgeRoundoff = Mth.clampedMap(
                            lowY, 0.0, generator.edgeRoundoffBegin(),
                            -generator.maxEdgeRoundoff(), 0.0);
                    double absToggleNoise = Math.abs(toggleNoise);
                    if (absToggleNoise + edgeRoundoff < generator.veininessThreshold()) {
                        continue;
                    }
                    if (random.nextFloat() > vein.density()) {
                        continue;
                    }
                    if (veinRidged.compute(context) >= 0.0) {
                        continue;
                    }

                    double chance = Mth.clampedMap(
                            absToggleNoise,
                            generator.veininessThreshold(),
                            generator.maxRichnessThreshold(),
                            generator.minRichness(),
                            generator.maxRichness());

                    // GT applies density and richness again through the per-block placer.
                    long randomSeed = random.nextLong();
                    RandomSource blockRandom = new XoroshiroRandomSource(randomSeed);
                    if (blockRandom.nextFloat() > vein.density()) {
                        continue;
                    }
                    if (blockRandom.nextFloat() >= chance) {
                        continue;
                    }

                    WeightedOre ore;
                    if (!generator.rare().isEmpty()
                            && blockRandom.nextFloat() < generator.rareChance()) {
                        ore = pick(generator.rare(), blockRandom);
                    } else {
                        ore = pick(generator.ores(), blockRandom);
                    }
                    put(result, level, new BlockPos(x, y, z), ore);
                }
            }
        }
        return result;
    }

    private static DensityFunction mapToNoise(DensityFunction function, RandomState randomState) {
        return function.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction densityFunction) {
                return densityFunction;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noiseHolder) {
                Holder<NormalNoise.NoiseParameters> holder = noiseHolder.noiseData();
                NormalNoise noise = randomState.getOrCreateNoise(holder.unwrapKey().orElseThrow());
                return new DensityFunction.NoiseHolder(holder, noise);
            }
        });
    }

    static Map<BlockPos, BlockState> standard(WorldGenLevel level, RandomSource random,
                                               VeinDefinition vein, BlockPos origin,
                                               VeinGenerator.Standard generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        int size = Math.max(1, vein.clusterSize(random));
        int radius = Math.max(1, size / 2);
        int yRadius = Math.max(1, size / 4);
        double xzRadiusSq = (double) radius * radius;
        double yRadiusSq = (double) yRadius * yRadius;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if ((double) x * x + (double) z * z > xzRadiusSq) {
                    continue;
                }
                for (int y = -yRadius; y <= yRadius; y++) {
                    if (level.isOutsideBuildHeight(origin.getY() + y)) {
                        continue;
                    }
                    if ((double) x * x / xzRadiusSq + (double) z * z / xzRadiusSq
                            + (double) y * y / yRadiusSq > 1.0) {
                        continue;
                    }
                    if (random.nextFloat() > vein.density()) {
                        continue;
                    }
                    put(result, level, origin.offset(x, y, z), pick(generator.ores(), random));
                }
            }
        }
        return result;
    }

    static Map<BlockPos, BlockState> geode(WorldGenLevel level, RandomSource random,
                                            VeinDefinition vein, BlockPos origin,
                                            VeinGenerator.Geode generator) {
        Map<BlockPos, BlockState> result = new LinkedHashMap<>();
        int size = Math.max(4, vein.clusterSize(random));
        double radius = size / 2.0;
        int ir = Mth.ceil(radius);

        for (int x = -ir; x <= ir; x++) {
            for (int y = -ir; y <= ir; y++) {
                if (level.isOutsideBuildHeight(origin.getY() + y)) {
                    continue;
                }
                for (int z = -ir; z <= ir; z++) {
                    double distance = Math.sqrt((double) x * x + (double) y * y + (double) z * z) / radius;
                    if (distance > 1.0) {
                        continue;
                    }
                    if (random.nextFloat() > vein.density()) {
                        continue;
                    }
                    VeinGenerator.Layer shell;
                    if (distance < 0.35) {
                        shell = generator.filling();
                    } else if (distance < 0.55) {
                        shell = generator.inner();
                    } else if (distance < 0.8) {
                        shell = generator.middle();
                    } else {
                        shell = generator.outer();
                    }
                    put(result, level, origin.offset(x, y, z), shell.pick(random));
                }
            }
        }
        return result;
    }

    static WeightedOre pick(List<WeightedOre> ores, RandomSource random) {
        if (ores == null || ores.isEmpty()) {
            return null;
        }
        int total = 0;
        for (WeightedOre ore : ores) {
            total += Math.max(1, ore.weight());
        }
        int roll = random.nextInt(total);
        for (WeightedOre ore : ores) {
            roll -= Math.max(1, ore.weight());
            if (roll < 0) {
                return ore;
            }
        }
        return ores.get(ores.size() - 1);
    }

    static boolean put(Map<BlockPos, BlockState> result, WorldGenLevel level,
                       BlockPos pos, WeightedOre ore) {
        if (ore == null || ore.state().isAir() || level.isOutsideBuildHeight(pos)) {
            return false;
        }
        result.put(pos.immutable(), ore.state());
        return true;
    }
}
