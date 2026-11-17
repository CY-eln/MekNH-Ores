package c_y.meknh_ores.worldgen.vein;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.Set;

/**
 * A GregTech-style world generation layer.  A layer determines which blocks a vein may
 * replace and which dimensions it is allowed to generate in.
 */
public record WorldGenLayer(ResourceLocation id, RuleTest target, Set<ResourceLocation> dimensions) {

    public WorldGenLayer {
        dimensions = Set.copyOf(dimensions);
    }

    public boolean appliesTo(ResourceLocation dimension) {
        return dimensions.contains(dimension);
    }
}
