package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.MekNHOres;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Fluent builder exposed to KubeJS through {@code MekNHOres.vein(id)}.
 *
 * <p>Defaults follow GregTech closely: one vein per applicable layer per grid node, a
 * GregTech-style generator, and optional surface indicators.  The old
 * {@code primary/secondary/sporadic} calls still work and produce a classic ellipsoid vein.</p>
 */
public final class OreVeinBuilder {

    private final ResourceLocation id;
    private int weight = 100;
    private int minClusterSize = 24;
    private int maxClusterSize = 24;
    private float density = 0.35f;
    private WorldGenLayer layer;
    private final Set<ResourceLocation> dimensions = new LinkedHashSet<>();
    private final List<String> biomes = new ArrayList<>();
    private final List<BiomeWeight> biomeWeights = new ArrayList<>();
    private int minY = -32;
    private int maxY = 64;
    private HeightRangePlacement heightRange = HeightRangePlacement.uniform(
            VerticalAnchor.absolute(-32), VerticalAnchor.absolute(64));
    private float discardChanceOnAirExposure;
    private final List<WeightedOre> primary = new ArrayList<>();
    private final List<WeightedOre> secondary = new ArrayList<>();
    private final List<WeightedOre> sporadic = new ArrayList<>();
    private final List<IndicatorDefinition> indicators = new ArrayList<>();
    private VeinGenerator generator;

    public OreVeinBuilder(String id) {
        this.id = WorldGenLayers.parseId(id);
    }

    public OreVeinBuilder weight(int weight) {
        this.weight = Math.max(1, weight);
        return this;
    }

    public OreVeinBuilder size(int size) {
        return clusterSize(size);
    }

    public OreVeinBuilder clusterSize(int size) {
        return clusterSize(size, size);
    }

    public OreVeinBuilder clusterSize(int min, int max) {
        this.minClusterSize = Math.max(1, Math.min(min, max));
        this.maxClusterSize = Math.max(1, Math.max(min, max));
        return this;
    }

    public OreVeinBuilder density(float density) {
        this.density = Math.max(0.0f, Math.min(1.0f, density));
        return this;
    }

    public OreVeinBuilder height(int minY, int maxY) {
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.heightRange = HeightRangePlacement.uniform(
                VerticalAnchor.absolute(this.minY), VerticalAnchor.absolute(this.maxY));
        return this;
    }

    public OreVeinBuilder heightTriangle(int minY, int maxY) {
        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);
        this.heightRange = HeightRangePlacement.triangle(
                VerticalAnchor.absolute(this.minY), VerticalAnchor.absolute(this.maxY));
        return this;
    }

    public OreVeinBuilder heightRangeUniform(int minY, int maxY) {
        return height(minY, maxY);
    }

    public OreVeinBuilder heightRangeTriangle(int minY, int maxY) {
        return heightTriangle(minY, maxY);
    }

    public OreVeinBuilder layer(String layerId) {
        WorldGenLayer resolved = WorldGenLayers.get(layerId);
        if (resolved == null) {
            MekNHOres.LOGGER.warn("Ignoring unknown vein layer: {}", layerId);
        } else {
            this.layer = resolved;
        }
        return this;
    }

    public OreVeinBuilder dimension(String dimension) {
        ResourceLocation parsed = ResourceLocation.tryParse(dimension);
        if (parsed != null) {
            dimensions.add(parsed);
        } else {
            MekNHOres.LOGGER.warn("Ignoring invalid vein dimension: {}", dimension);
        }
        return this;
    }

    public OreVeinBuilder dimensions(String... dimensions) {
        if (dimensions != null) {
            for (String dimension : dimensions) {
                dimension(dimension);
            }
        }
        return this;
    }

    public OreVeinBuilder biome(String biome) {
        if (biome != null && !biome.isBlank()) {
            biomes.add(biome.trim());
        }
        return this;
    }

    public OreVeinBuilder biomes(String... biomes) {
        if (biomes != null) {
            for (String biome : biomes) {
                biome(biome);
            }
        }
        return this;
    }

    public OreVeinBuilder biomeWeight(String filter, int addedWeight) {
        if (filter != null && !filter.isBlank()) {
            biomeWeights.add(new BiomeWeight(filter.trim(), addedWeight));
        }
        return this;
    }

    public OreVeinBuilder discardChanceOnAirExposure(float chance) {
        this.discardChanceOnAirExposure = Math.max(0.0f, Math.min(1.0f, chance));
        return this;
    }

    public OreVeinBuilder primary(String blockId, int weight) {
        add(primary, blockId, weight);
        return this;
    }

    public OreVeinBuilder primary(String blockId) {
        return primary(blockId, 1);
    }

    public OreVeinBuilder secondary(String blockId, int weight) {
        add(secondary, blockId, weight);
        return this;
    }

    public OreVeinBuilder secondary(String blockId) {
        return secondary(blockId, 1);
    }

    public OreVeinBuilder sporadic(String blockId, int weight) {
        add(sporadic, blockId, weight);
        return this;
    }

    public OreVeinBuilder sporadic(String blockId) {
        return sporadic(blockId, 1);
    }

    public OreVeinBuilder layeredVeinGenerator(Consumer<LayeredBuilder> builder) {
        LayeredBuilder layered = new LayeredBuilder();
        builder.accept(layered);
        this.generator = new VeinGenerator.Layered(layered.build());
        return this;
    }

    public OreVeinBuilder classicVeinGenerator(Consumer<ClassicBuilder> builder) {
        ClassicBuilder classic = new ClassicBuilder();
        builder.accept(classic);
        this.generator = classic.build();
        return this;
    }

    public OreVeinBuilder cuboidVeinGenerator(Consumer<CuboidBuilder> builder) {
        CuboidBuilder cuboid = new CuboidBuilder();
        builder.accept(cuboid);
        this.generator = cuboid.build(minY, maxY);
        return this;
    }

    public OreVeinBuilder dikeVeinGenerator(Consumer<DikeBuilder> builder) {
        DikeBuilder dike = new DikeBuilder();
        builder.accept(dike);
        this.generator = dike.build();
        return this;
    }

    public OreVeinBuilder veinedVeinGenerator(Consumer<VeinedBuilder> builder) {
        VeinedBuilder veined = new VeinedBuilder();
        builder.accept(veined);
        this.generator = veined.build();
        return this;
    }

    public OreVeinBuilder standardVeinGenerator(Consumer<StandardBuilder> builder) {
        StandardBuilder standard = new StandardBuilder();
        builder.accept(standard);
        this.generator = standard.build();
        return this;
    }

    public OreVeinBuilder geodeVeinGenerator(Consumer<GeodeBuilder> builder) {
        GeodeBuilder geode = new GeodeBuilder();
        builder.accept(geode);
        this.generator = geode.build();
        return this;
    }

    public OreVeinBuilder surfaceIndicator(Consumer<SurfaceIndicatorBuilder> builder) {
        SurfaceIndicatorBuilder indicator = new SurfaceIndicatorBuilder();
        builder.accept(indicator);
        this.indicators.add(indicator.build());
        return this;
    }

    public OreVeinBuilder indicator(Consumer<SurfaceIndicatorBuilder> builder) {
        return surfaceIndicator(builder);
    }

    public void register() {
        List<WeightedOre> explicitOres = generator == null ? List.of() : generator.allOres();
        if (generator == null) {
            if (primary.isEmpty() && secondary.isEmpty() && sporadic.isEmpty()) {
                MekNHOres.LOGGER.warn("Ignoring vein {} because it has no ore blocks or generator", id);
                return;
            }
            generator = new VeinGenerator.Legacy(primary, secondary, sporadic);
        } else if (explicitOres.isEmpty()) {
            MekNHOres.LOGGER.warn("Ignoring vein {} because its generator has no ore blocks", id);
            return;
        }

        WorldGenLayer resolvedLayer = resolveLayer();
        VeinDefinition definition = new VeinDefinition(
                id, weight, minClusterSize, maxClusterSize, density, resolvedLayer,
                dimensions, biomes, biomeWeights, minY, maxY, heightRange,
                discardChanceOnAirExposure, primary, secondary, sporadic, generator, indicators);
        VeinData.register(definition);
        MekNHOres.LOGGER.info("Registered KubeJS ore vein {} (layer {}, {} ore entries, generator {})",
                id, resolvedLayer.id(), definition.allOres().size(), generator.type());
    }

    private WorldGenLayer resolveLayer() {
        if (layer != null) {
            return layer;
        }
        if (dimensions.contains(Level.NETHER.location())) {
            return WorldGenLayers.NETHERRACK;
        }
        if (dimensions.contains(Level.END.location())) {
            return WorldGenLayers.ENDSTONE;
        }
        for (WeightedOre ore : generator.allOres()) {
            if (ore.id().getPath().contains("deepslate")) {
                return WorldGenLayers.DEEPSLATE;
            }
        }
        return WorldGenLayers.STONE;
    }

    private static void add(List<WeightedOre> target, String blockId, int weight) {
        WeightedOre ore = ore(blockId, weight);
        if (ore != null) {
            target.add(ore);
        }
    }

    static WeightedOre ore(String blockId, int weight) {
        ResourceLocation parsed = parseBlockId(blockId);
        if (parsed == null) {
            MekNHOres.LOGGER.warn("Ignoring invalid vein ore block: {}", blockId);
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(parsed);
        BlockState state = block.defaultBlockState();
        if (state.isAir()) {
            MekNHOres.LOGGER.warn("Ignoring unknown vein ore block: {}", parsed);
            return null;
        }
        return new WeightedOre(parsed, state, Math.max(1, weight));
    }

    static BlockState state(String blockId) {
        WeightedOre ore = ore(blockId, 1);
        return ore == null ? null : ore.state();
    }

    static ResourceLocation parseBlockId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        return blockId.indexOf(':') >= 0
                ? ResourceLocation.tryParse(blockId)
                : ResourceLocation.withDefaultNamespace(blockId);
    }

    public static final class LayerBuilder {

        private final List<WeightedOre> ores = new ArrayList<>();
        private int minSize = 1;
        private int maxSize = 1;
        private int weight = 1;

        public LayerBuilder block(String blockId) {
            return block(blockId, 1);
        }

        public LayerBuilder block(String blockId, int weight) {
            WeightedOre ore = OreVeinBuilder.ore(blockId, weight);
            if (ore != null) {
                ores.add(ore);
            }
            return this;
        }

        public LayerBuilder size(int size) {
            return size(size, size);
        }

        public LayerBuilder size(int min, int max) {
            this.minSize = Math.max(1, Math.min(min, max));
            this.maxSize = Math.max(1, Math.max(min, max));
            return this;
        }

        public LayerBuilder weight(int weight) {
            this.weight = Math.max(1, weight);
            return this;
        }

        public VeinGenerator.Layer build() {
            return new VeinGenerator.Layer(ores, minSize, maxSize, weight);
        }
    }

    public static final class LayeredBuilder {

        private final List<VeinGenerator.LayerPattern> patterns = new ArrayList<>();
        private final List<VeinGenerator.Layer> current = new ArrayList<>();

        public LayeredBuilder layer(Consumer<LayerBuilder> builder) {
            LayerBuilder layer = new LayerBuilder();
            builder.accept(layer);
            current.add(layer.build());
            return this;
        }

        public LayeredBuilder nextPattern() {
            if (!current.isEmpty()) {
                patterns.add(new VeinGenerator.LayerPattern(current));
                current.clear();
            }
            return this;
        }

        List<VeinGenerator.LayerPattern> build() {
            nextPattern();
            return patterns;
        }
    }

    public static final class ClassicBuilder {

        private VeinGenerator.Layer primary;
        private VeinGenerator.Layer secondary;
        private VeinGenerator.Layer between;
        private VeinGenerator.Layer sporadic;
        private int yRadius = 6;

        public ClassicBuilder primary(Consumer<LayerBuilder> builder) {
            this.primary = layer(builder, 4);
            return this;
        }

        public ClassicBuilder secondary(Consumer<LayerBuilder> builder) {
            this.secondary = layer(builder, 3);
            return this;
        }

        public ClassicBuilder between(Consumer<LayerBuilder> builder) {
            this.between = layer(builder, 3);
            return this;
        }

        public ClassicBuilder sporadic(Consumer<LayerBuilder> builder) {
            this.sporadic = layer(builder, 1);
            return this;
        }

        public ClassicBuilder yRadius(int yRadius) {
            this.yRadius = Math.max(1, yRadius);
            return this;
        }

        VeinGenerator build() {
            return new VeinGenerator.Classic(
                    orEmpty(primary, 4), orEmpty(secondary, 3), orEmpty(between, 3),
                    orEmpty(sporadic, 1), yRadius);
        }

        private static VeinGenerator.Layer layer(Consumer<LayerBuilder> builder, int defaultSize) {
            LayerBuilder layer = new LayerBuilder().size(defaultSize);
            builder.accept(layer);
            return layer.build();
        }

        private static VeinGenerator.Layer orEmpty(VeinGenerator.Layer layer, int defaultSize) {
            return layer == null ? new VeinGenerator.Layer(List.of(), defaultSize, defaultSize, 1) : layer;
        }
    }

    public static final class CuboidBuilder {

        private VeinGenerator.Layer top;
        private VeinGenerator.Layer middle;
        private VeinGenerator.Layer bottom;
        private VeinGenerator.Layer spread;

        public CuboidBuilder top(Consumer<LayerBuilder> builder) {
            this.top = layer(builder, 2);
            return this;
        }

        public CuboidBuilder middle(Consumer<LayerBuilder> builder) {
            this.middle = layer(builder, 3);
            return this;
        }

        public CuboidBuilder bottom(Consumer<LayerBuilder> builder) {
            this.bottom = layer(builder, 2);
            return this;
        }

        public CuboidBuilder spread(Consumer<LayerBuilder> builder) {
            this.spread = layer(builder, 1);
            return this;
        }

        VeinGenerator build(int minY, int maxY) {
            return new VeinGenerator.Cuboid(
                    orEmpty(top, 2), orEmpty(middle, 3), orEmpty(bottom, 2), orEmpty(spread, 1),
                    minY, maxY);
        }

        private static VeinGenerator.Layer layer(Consumer<LayerBuilder> builder, int defaultSize) {
            LayerBuilder layer = new LayerBuilder().size(defaultSize);
            builder.accept(layer);
            return layer.build();
        }

        private static VeinGenerator.Layer orEmpty(VeinGenerator.Layer layer, int defaultSize) {
            return layer == null ? new VeinGenerator.Layer(List.of(), defaultSize, defaultSize, 1) : layer;
        }
    }

    public static final class DikeBuilder {

        private final List<VeinGenerator.DikeBlock> blocks = new ArrayList<>();

        public DikeBuilder block(String blockId, int weight, int minY, int maxY) {
            WeightedOre ore = OreVeinBuilder.ore(blockId, weight);
            if (ore != null) {
                blocks.add(new VeinGenerator.DikeBlock(ore, minY, maxY));
            }
            return this;
        }

        public DikeBuilder block(String blockId, int minY, int maxY) {
            return block(blockId, 1, minY, maxY);
        }

        VeinGenerator build() {
            return new VeinGenerator.Dike(blocks);
        }
    }

    public static final class VeinedBuilder {

        private final List<WeightedOre> ores = new ArrayList<>();
        private final List<WeightedOre> rare = new ArrayList<>();
        private float rareChance = 0.02f;
        // These are the parameters GregTech actually uses for its veined ore definitions
        // (see GTOres: veininessThreshold(0.01), maxRichnessThreshold(0.175),
        // minRichness(0.7), maxRichness(1.0), edgeRoundoffBegin(3), maxEdgeRoundoff(0.1)).
        // The codec defaults (0.4 / 0.6 / 0.1 / 0.3) are far too strict and produce almost
        // no blocks, especially when density is also below 1.0.
        private float veininessThreshold = 0.01f;
        private int edgeRoundoffBegin = 3;
        private double maxEdgeRoundoff = 0.1;
        private float minRichness = 0.7f;
        private float maxRichness = 1.0f;
        private float maxRichnessThreshold = 0.175f;

        public VeinedBuilder oreBlock(String blockId, int weight) {
            WeightedOre ore = OreVeinBuilder.ore(blockId, weight);
            if (ore != null) {
                ores.add(ore);
            }
            return this;
        }

        public VeinedBuilder rareBlock(String blockId, int weight) {
            WeightedOre ore = OreVeinBuilder.ore(blockId, weight);
            if (ore != null) {
                rare.add(ore);
            }
            return this;
        }

        public VeinedBuilder rareBlockChance(float chance) {
            this.rareChance = chance;
            return this;
        }

        public VeinedBuilder veininessThreshold(float value) {
            this.veininessThreshold = value;
            return this;
        }

        public VeinedBuilder edgeRoundoffBegin(int value) {
            this.edgeRoundoffBegin = Math.max(1, value);
            return this;
        }

        public VeinedBuilder maxEdgeRoundoff(double value) {
            this.maxEdgeRoundoff = value;
            return this;
        }

        public VeinedBuilder minRichness(float value) {
            this.minRichness = value;
            return this;
        }

        public VeinedBuilder maxRichness(float value) {
            this.maxRichness = value;
            return this;
        }

        public VeinedBuilder maxRichnessThreshold(float value) {
            this.maxRichnessThreshold = value;
            return this;
        }

        VeinGenerator build() {
            return new VeinGenerator.Veined(ores, rare, rareChance, veininessThreshold,
                    edgeRoundoffBegin, maxEdgeRoundoff, minRichness, maxRichness,
                    maxRichnessThreshold);
        }
    }

    public static final class StandardBuilder {

        private final List<WeightedOre> ores = new ArrayList<>();

        public StandardBuilder block(String blockId, int weight) {
            WeightedOre ore = OreVeinBuilder.ore(blockId, weight);
            if (ore != null) {
                ores.add(ore);
            }
            return this;
        }

        public StandardBuilder block(String blockId) {
            return block(blockId, 1);
        }

        VeinGenerator build() {
            return new VeinGenerator.Standard(ores);
        }
    }

    public static final class GeodeBuilder {

        private VeinGenerator.Layer filling;
        private VeinGenerator.Layer inner;
        private VeinGenerator.Layer middle;
        private VeinGenerator.Layer outer;

        public GeodeBuilder filling(Consumer<LayerBuilder> builder) {
            this.filling = layer(builder);
            return this;
        }

        public GeodeBuilder inner(Consumer<LayerBuilder> builder) {
            this.inner = layer(builder);
            return this;
        }

        public GeodeBuilder middle(Consumer<LayerBuilder> builder) {
            this.middle = layer(builder);
            return this;
        }

        public GeodeBuilder outer(Consumer<LayerBuilder> builder) {
            this.outer = layer(builder);
            return this;
        }

        VeinGenerator build() {
            return new VeinGenerator.Geode(
                    orEmpty(filling), orEmpty(inner), orEmpty(middle), orEmpty(outer));
        }

        private static VeinGenerator.Layer layer(Consumer<LayerBuilder> builder) {
            LayerBuilder layer = new LayerBuilder();
            builder.accept(layer);
            return layer.build();
        }

        private static VeinGenerator.Layer orEmpty(VeinGenerator.Layer layer) {
            return layer == null ? new VeinGenerator.Layer(List.of(), 1, 1, 1) : layer;
        }
    }

    public static final class SurfaceIndicatorBuilder {

        private BlockState state;
        private int radius = 5;
        private float density = 0.2f;
        private IndicatorDefinition.Placement placement = IndicatorDefinition.Placement.SURFACE;

        public SurfaceIndicatorBuilder block(String blockId) {
            BlockState parsed = OreVeinBuilder.state(blockId);
            if (parsed != null) {
                this.state = parsed;
            }
            return this;
        }

        public SurfaceIndicatorBuilder radius(int radius) {
            this.radius = Math.max(1, Math.min(32, radius));
            return this;
        }

        public SurfaceIndicatorBuilder density(float density) {
            this.density = Math.max(0.0f, Math.min(1.0f, density));
            return this;
        }

        public SurfaceIndicatorBuilder placement(String placement) {
            this.placement = IndicatorDefinition.Placement.parse(placement);
            return this;
        }

        IndicatorDefinition build() {
            if (state == null) {
                throw new IllegalStateException("Surface indicator needs a block");
            }
            return new IndicatorDefinition(state, radius, density, placement);
        }
    }
}
