package c_y.meknh_ores.worldgen.vein;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GregTech-style surface indicator: a patch of a chosen block around the vein center,
 * placed on / above / below the surface.
 */
public record IndicatorDefinition(BlockState state, int radius, float density, Placement placement) {

    public enum Placement {
        SURFACE,
        ABOVE,
        BELOW;

        public static Placement parse(String value) {
            if (value == null) {
                return SURFACE;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "above" -> ABOVE;
                case "below" -> BELOW;
                default -> SURFACE;
            };
        }
    }

    /**
     * Candidate (x, y, z) positions around the vein center.  The final Y is resolved when the
     * containing chunk is generated, because the heightmap is not reliable for ungenerated chunks.
     */
    public List<BlockPos> candidates(BlockPos center, RandomSource random) {
        int r = Math.max(1, Math.min(32, radius));
        List<BlockPos> result = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if ((double) dx * dx + (double) dz * dz > (double) r * r) {
                    continue;
                }
                if ((dx != 0 || dz != 0) && random.nextFloat() > density) {
                    continue;
                }
                result.add(new BlockPos(center.getX() + dx, center.getY(), center.getZ() + dz));
            }
        }
        return result;
    }

    public BlockPos resolve(WorldGenLevel level, BlockPos candidate) {
        return switch (placement) {
            case SURFACE -> candidate.atY(Math.max(
                    level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, candidate.getX(), candidate.getZ()),
                    candidate.getY()));
            case ABOVE -> find(level, candidate, Direction.UP);
            case BELOW -> find(level, candidate, Direction.DOWN);
        };
    }

    private static BlockPos find(WorldGenLevel level, BlockPos start, Direction direction) {
        int limit = direction == Direction.UP
                ? level.getMaxBuildHeight()
                : level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = start.mutable();
        int steps = Math.abs(limit - start.getY());
        for (int i = 0; i < steps; i++) {
            BlockPos below = pos.relative(direction.getOpposite());
            if (level.getBlockState(pos).isAir()
                    && level.getBlockState(below).isFaceSturdy(level, below, direction.getOpposite())) {
                return pos.immutable();
            }
            pos.move(direction);
        }
        return start;
    }
}
