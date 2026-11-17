package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.OresConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A fully generated vein, cached by its origin chunk.  Blocks are grouped by chunk so a chunk
 * only pays for the part of the vein that intersects it.
 */
public final class GeneratedVein {

    private final ChunkPos origin;
    private final VeinDefinition definition;
    private final Map<BlockPos, BlockState> blocks;
    private final Map<ChunkPos, Map<BlockPos, BlockState>> blocksByChunk;
    private final List<IndicatorPatch> indicators;

    public GeneratedVein(ChunkPos origin, VeinDefinition definition,
                         Map<BlockPos, BlockState> blocks,
                         List<IndicatorPatch> indicators) {
        this.origin = origin;
        this.definition = definition;
        this.blocks = Map.copyOf(blocks);
        this.indicators = List.copyOf(indicators);

        Map<ChunkPos, Map<BlockPos, BlockState>> grouped = new HashMap<>();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            ChunkPos chunk = new ChunkPos(entry.getKey());
            grouped.computeIfAbsent(chunk, ignored -> new HashMap<>())
                    .put(entry.getKey(), entry.getValue());
        }
        this.blocksByChunk = grouped;
    }

    public ChunkPos origin() {
        return origin;
    }

    public VeinDefinition definition() {
        return definition;
    }

    public Map<BlockPos, BlockState> blocks() {
        return blocks;
    }

    public int placeInChunk(WorldGenLevel level, ChunkPos chunk, RandomSource random) {
        int placed = 0;
        for (Map.Entry<BlockPos, BlockState> entry
                : blocksByChunk.getOrDefault(chunk, Map.of()).entrySet()) {
            BlockPos pos = entry.getKey();
            if (level.isOutsideBuildHeight(pos)) {
                continue;
            }

            BlockState current = level.getBlockState(pos);
            if (!definition.layer().target().test(current, random)) {
                continue;
            }
            if (definition.discardChanceOnAirExposure() > 0
                    && isAdjacentToAir(level, pos)
                    && random.nextFloat() < definition.discardChanceOnAirExposure()) {
                continue;
            }
            level.setBlock(pos, entry.getValue(), Block.UPDATE_CLIENTS);
            placed++;
        }

        if (OresConfig.veinIndicators()) {
            placed += placeIndicators(level, chunk);
        }
        return placed;
    }

    private int placeIndicators(WorldGenLevel level, ChunkPos chunk) {
        int placed = 0;
        for (IndicatorPatch patch : indicators) {
            for (BlockPos candidate : patch.candidates()) {
                if ((candidate.getX() >> 4) != chunk.x || (candidate.getZ() >> 4) != chunk.z) {
                    continue;
                }
                BlockPos resolved = patch.definition().resolve(level, candidate);
                if (level.isOutsideBuildHeight(resolved) || !level.getBlockState(resolved).isAir()) {
                    continue;
                }
                level.setBlock(resolved, patch.definition().state(), Block.UPDATE_CLIENTS);
                placed++;
            }
        }
        return placed;
    }

    private static boolean isAdjacentToAir(WorldGenLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).isAir()) {
                return true;
            }
        }
        return false;
    }

    public record IndicatorPatch(IndicatorDefinition definition, List<BlockPos> candidates) {

        public IndicatorPatch {
            candidates = List.copyOf(candidates);
        }
    }
}
