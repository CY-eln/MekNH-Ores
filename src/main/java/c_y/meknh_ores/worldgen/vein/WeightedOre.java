package c_y.meknh_ores.worldgen.vein;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public record WeightedOre(ResourceLocation id, BlockState state, int weight) {
}
