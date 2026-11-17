package c_y.meknh_ores.worldgen.vein;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

/**
 * Additional selection weight for a biome or biome tag.
 */
public record BiomeWeight(String filter, int addedWeight) {

    public boolean matches(Holder<Biome> biome) {
        return VeinDefinition.matchesBiomeFilter(filter, biome);
    }
}
