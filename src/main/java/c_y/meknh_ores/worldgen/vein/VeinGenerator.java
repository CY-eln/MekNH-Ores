package c_y.meknh_ores.worldgen.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shape generator for a vein.  Implementations only compute the blocks; placement is done
 * later, when the chunk containing a block is actually generated.
 */
public interface VeinGenerator {

    Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                       VeinDefinition vein, BlockPos origin);

    List<WeightedOre> allOres();

    default String type() {
        return getClass().getSimpleName();
    }

    @SafeVarargs
    static List<WeightedOre> union(List<WeightedOre>... lists) {
        List<WeightedOre> result = new ArrayList<>();
        for (List<WeightedOre> list : lists) {
            if (list != null) {
                result.addAll(list);
            }
        }
        return List.copyOf(result);
    }

    record Layer(List<WeightedOre> ores, int minSize, int maxSize, int weight) {

        public Layer {
            ores = List.copyOf(ores);
            if (minSize > maxSize) {
                int swap = minSize;
                minSize = maxSize;
                maxSize = swap;
            }
            minSize = Math.max(0, minSize);
            maxSize = Math.max(minSize, maxSize);
            weight = Math.max(1, weight);
        }

        public WeightedOre pick(RandomSource random) {
            if (ores.isEmpty()) {
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

        public int size(RandomSource random) {
            if (maxSize <= minSize) {
                return minSize;
            }
            return minSize + random.nextInt(maxSize - minSize + 1);
        }

        public List<WeightedOre> allOres() {
            return ores;
        }
    }

    record LayerPattern(List<Layer> layers) {

        public LayerPattern {
            layers = List.copyOf(layers);
        }

        public Layer rollNext(Layer previous, RandomSource random) {
            if (layers.isEmpty()) {
                return null;
            }
            if (layers.size() == 1) {
                return layers.get(0);
            }
            int total = 0;
            for (Layer layer : layers) {
                if (layer != previous) {
                    total += layer.weight();
                }
            }
            if (total <= 0) {
                return null;
            }
            int roll = random.nextInt(total);
            for (Layer layer : layers) {
                if (layer == previous) {
                    continue;
                }
                roll -= layer.weight();
                if (roll < 0) {
                    return layer;
                }
            }
            return null;
        }
    }

    record Layered(List<LayerPattern> patterns) implements VeinGenerator {

        public Layered {
            patterns = List.copyOf(patterns);
        }

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.layered(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            List<WeightedOre> result = new ArrayList<>();
            for (LayerPattern pattern : patterns) {
                for (Layer layer : pattern.layers()) {
                    result.addAll(layer.ores());
                }
            }
            return List.copyOf(result);
        }
    }

    record Classic(Layer primary, Layer secondary, Layer between, Layer sporadic, int yRadius)
            implements VeinGenerator {

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.classic(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            return union(primary.ores(), secondary.ores(), between.ores(), sporadic.ores());
        }
    }

    record Cuboid(Layer top, Layer middle, Layer bottom, Layer spread, int minY, int maxY)
            implements VeinGenerator {

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.cuboid(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            return union(top.ores(), middle.ores(), bottom.ores(), spread.ores());
        }
    }

    record Dike(List<DikeBlock> blocks) implements VeinGenerator {

        public Dike {
            blocks = List.copyOf(blocks);
        }

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.dike(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            List<WeightedOre> result = new ArrayList<>();
            for (DikeBlock block : blocks) {
                result.add(block.ore());
            }
            return List.copyOf(result);
        }
    }

    record DikeBlock(WeightedOre ore, int minY, int maxY) {

        public DikeBlock {
            if (minY > maxY) {
                int swap = minY;
                minY = maxY;
                maxY = swap;
            }
        }
    }

    record Veined(List<WeightedOre> ores, List<WeightedOre> rare, float rareChance,
                  float veininessThreshold, int edgeRoundoffBegin, double maxEdgeRoundoff,
                  float minRichness, float maxRichness, float maxRichnessThreshold)
            implements VeinGenerator {

        public Veined {
            ores = List.copyOf(ores);
            rare = List.copyOf(rare);
        }

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.veined(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            return union(ores, rare);
        }
    }

    record Standard(List<WeightedOre> ores) implements VeinGenerator {

        public Standard {
            ores = List.copyOf(ores);
        }

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.standard(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            return ores;
        }
    }

    record Geode(Layer filling, Layer inner, Layer middle, Layer outer) implements VeinGenerator {

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.geode(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            return union(filling.ores(), inner.ores(), middle.ores(), outer.ores());
        }
    }

    record Legacy(List<WeightedOre> primary, List<WeightedOre> secondary, List<WeightedOre> sporadic)
            implements VeinGenerator {

        public Legacy {
            primary = List.copyOf(primary);
            secondary = List.copyOf(secondary);
            sporadic = List.copyOf(sporadic);
        }

        @Override
        public Map<BlockPos, BlockState> generate(WorldGenLevel level, RandomSource random,
                                                  VeinDefinition vein, BlockPos origin) {
            return VeinGeneratorEngine.legacy(level, random, vein, origin, this);
        }

        @Override
        public List<WeightedOre> allOres() {
            return union(primary, secondary, sporadic);
        }
    }
}
