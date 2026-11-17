package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.OresConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Places the parts of all veins intersecting the current chunk.
 *
 * <p>Veins are generated from their grid-node origin chunk and cached, so a single vein can
 * span several chunks without being clipped at chunk borders.</p>
 */
public final class OreVeinFeature extends Feature<NoneFeatureConfiguration> {

    private static final Set<String> ORIGINAL_ORE_MATERIALS = Set.of(
            "copper", "tin", "iron", "gold", "silver", "lead", "zinc", "nickel",
            "platinum", "uranium", "tungsten", "aluminum", "titanium", "osmium",
            "sulfur", "fluorite", "diamond", "redstone", "lapis", "coal", "quartz", "glowstone"
    );

    private static boolean logged;
    private static long clearedBlocks;

    public OreVeinFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!OresConfig.enableVeinTakeover() || !VeinData.hasVeins()) {
            return false;
        }

        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        if (!VeinData.hasVeinsForDimension(level.getLevel().dimension().location())) {
            return false;
        }
        RandomSource random = new XoroshiroRandomSource(level.getSeed() ^ chunk.toLong());

        // The feature is appended to the end of UNDERGROUND_ORES, after vanilla/Mekanism/Ad Astra
        // ore features.  Remove any original ore block in this chunk before placing the GT vein.
        // This is deliberately block-based: biome feature removal alone can be defeated by a
        // stale FeatureSorter or by a mod that injects ores outside the biome feature list.
        clearOriginalOres(level, chunk);

        List<GeneratedVein> veins = VeinData.veinsForChunk(level, context.chunkGenerator(), chunk);
        int placed = 0;
        for (GeneratedVein vein : veins) {
            placed += vein.placeInChunk(level, chunk, random);
        }
        if (placed > 0 && !logged) {
            logged = true;
            c_y.meknh_ores.MekNHOres.LOGGER.info(
                    "Ore vein feature placed {} block(s) in chunk {} (cleared {} original ore block(s) so far)",
                    placed, chunk, clearedBlocks);
        }
        return placed > 0;
    }

    private static void clearOriginalOres(WorldGenLevel level, ChunkPos chunk) {
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int cleared = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = minY; y < maxY; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!isOriginalOre(state)) {
                        continue;
                    }
                    level.setBlock(pos, fillerFor(level, y), Block.UPDATE_CLIENTS);
                    cleared++;
                }
            }
        }
        clearedBlocks += cleared;
        if (cleared > 0) {
            c_y.meknh_ores.MekNHOres.LOGGER.debug("Cleared {} original ore block(s) in chunk {}",
                    cleared, chunk);
        }
    }

    private static boolean isOriginalOre(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (path.equals("glowstone")) {
            return true;
        }
        String material = path;
        if (material.startsWith("raw_") && material.endsWith("_block")) {
            material = material.substring("raw_".length(), material.length() - "_block".length());
            return ORIGINAL_ORE_MATERIALS.contains(material);
        }
        if (!path.endsWith("_ore")) {
            return false;
        }
        for (String prefix : List.of("deepslate_", "nether_", "end_", "moon_", "mars_")) {
            if (material.startsWith(prefix)) {
                material = material.substring(prefix.length());
                break;
            }
        }
        if (material.endsWith("_ore")) {
            material = material.substring(0, material.length() - 4);
        }
        return ORIGINAL_ORE_MATERIALS.contains(material);
    }

    private static BlockState fillerFor(WorldGenLevel level, int y) {
        ResourceLocation dimension = level.getLevel().dimension().location();
        if (dimension.equals(Level.NETHER.location())) {
            return Blocks.NETHERRACK.defaultBlockState();
        }
        if (dimension.equals(Level.END.location())) {
            return Blocks.END_STONE.defaultBlockState();
        }
        if (dimension.getPath().contains("moon")) {
            Block moon = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("ad_astra:moon_stone"));
            return moon == Blocks.AIR ? Blocks.STONE.defaultBlockState() : moon.defaultBlockState();
        }
        if (dimension.getPath().contains("mars")) {
            Block mars = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse("ad_astra:mars_stone"));
            return mars == Blocks.AIR ? Blocks.STONE.defaultBlockState() : mars.defaultBlockState();
        }
        return y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
    }
}
