package c_y.meknh_ores.worldgen.vein;

import c_y.meknh_ores.MekNHOres;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * KubeJS builder for custom world generation layers:
 *
 * <pre>{@code
 * MekNHOres.layer('my_layer')
 *     .targets('#minecraft:stone_ore_replaceables', 'minecraft:end_stone')
 *     .dimensions('minecraft:overworld')
 *     .register()
 * }</pre>
 */
public final class WorldGenLayerBuilder {

    private final ResourceLocation id;
    private final List<String> targets = new ArrayList<>();
    private final Set<ResourceLocation> dimensions = new LinkedHashSet<>();

    public WorldGenLayerBuilder(String id) {
        this.id = WorldGenLayers.parseId(id);
    }

    public WorldGenLayerBuilder target(String target) {
        if (target != null && !target.isBlank()) {
            targets.add(target.trim());
        }
        return this;
    }

    public WorldGenLayerBuilder targets(String... targets) {
        if (targets != null) {
            for (String target : targets) {
                target(target);
            }
        }
        return this;
    }

    public WorldGenLayerBuilder dimension(String dimension) {
        ResourceLocation parsed = ResourceLocation.tryParse(dimension);
        if (parsed != null) {
            dimensions.add(parsed);
        } else {
            MekNHOres.LOGGER.warn("Ignoring invalid layer dimension: {}", dimension);
        }
        return this;
    }

    public WorldGenLayerBuilder dimensions(String... dimensions) {
        if (dimensions != null) {
            for (String dimension : dimensions) {
                dimension(dimension);
            }
        }
        return this;
    }

    public WorldGenLayer register() {
        if (targets.isEmpty()) {
            throw new IllegalStateException("World generation layer " + id + " has no targets");
        }
        Set<ResourceLocation> levels = dimensions.isEmpty()
                ? Set.of(ResourceLocation.withDefaultNamespace("overworld"))
                : Set.copyOf(dimensions);
        WorldGenLayer layer = WorldGenLayers.register(id.toString(), WorldGenLayers.combinedRuleFor(targets), levels);
        MekNHOres.LOGGER.info("Registered KubeJS world generation layer {} with {} target(s)",
                id, targets.size());
        return layer;
    }
}
