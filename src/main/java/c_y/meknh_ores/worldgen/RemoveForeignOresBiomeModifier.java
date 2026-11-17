package c_y.meknh_ores.worldgen;

import com.mojang.serialization.MapCodec;
import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.OresConfig;
import c_y.meknh_ores.resource.UnifiedResourceIndex;
import c_y.meknh_ores.worldgen.vein.VeinData;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Removes placed features that generate ore blocks only when the ore material/block is on the
 * cleanup whitelist.  Everything not explicitly whitelisted is left alone, so fantasy ores such as
 * aether are never removed by accident.
 *
 * <p>The modifier has no configuration fields and is applied to every biome.  It runs in
 * {@link BiomeModifier.Phase#AFTER_EVERYTHING} so features added by other biome modifiers are
 * already present, including features from datapacks.</p>
 */
public final class RemoveForeignOresBiomeModifier implements BiomeModifier {
    public static final RemoveForeignOresBiomeModifier INSTANCE = new RemoveForeignOresBiomeModifier();
    public static final MapCodec<RemoveForeignOresBiomeModifier> CODEC = MapCodec.unit(INSTANCE);

    private RemoveForeignOresBiomeModifier() {
    }

    private static boolean loggedTakeoverBlocks;

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING) {
            return;
        }
        if (!OresConfig.disableForeignOreGeneration() && !OresConfig.enableVeinTakeover()) {
            return;
        }

        if (!loggedTakeoverBlocks) {
            loggedTakeoverBlocks = true;
            Set<ResourceLocation> blocks = VeinData.takenOverBlocks();
            MekNHOres.LOGGER.info("Ore-feature remover sees {} takeover block(s): {}", blocks.size(), blocks);
        }

        int removed = 0;
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(step);
            if (features.isEmpty()) {
                continue;
            }
            int before = features.size();
            features.removeIf(RemoveForeignOresBiomeModifier::isCleanupOreFeature);
            int count = before - features.size();
            removed += count;
        }

        if (removed > 0) {
            MekNHOres.LOGGER.debug("Removed {} matched ore feature(s) from biome {}",
                    removed, biome.unwrapKey().map(ResourceKey::location).orElse(null));
        }
    }

    private static boolean isCleanupOreFeature(Holder<PlacedFeature> holder) {
        ResourceKey<PlacedFeature> key = holder.unwrapKey().orElse(null);
        ResourceLocation featureId = key == null ? null : key.location();
        boolean takeover = OresConfig.enableVeinTakeover();
        boolean cleanup = OresConfig.disableForeignOreGeneration();
        UnifiedResourceIndex index = UnifiedResourceIndex.get();
        Set<ResourceLocation> takeoverBlocks = takeover ? VeinData.takenOverBlocks() : Set.of();

        boolean foundOreConfig = false;
        for (ConfiguredFeature<?, ?> configured : holder.value().getFeatures().toList()) {
            List<OreConfiguration.TargetBlockState> targets = targetStates(configured.config());
            if (!targets.isEmpty()) {
                foundOreConfig = true;
                for (OreConfiguration.TargetBlockState target : targets) {
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(target.state.getBlock());
                    if (takeover && takeoverBlocks.contains(blockId)) {
                        logRemoval(featureId, blockId, "target block");
                        return true;
                    }
                    if (cleanup && index.isCleanupBlock(blockId)) {
                        logRemoval(featureId, blockId, "cleanup block");
                        return true;
                    }
                }
            }

            // Some modded ore features hide their targets from us (or use a config type that
            // does not expose targetStates()).  Their config's toString() still contains the
            // block-state ids, so use that as a last-resort match.  This is what catches
            // Mekanism's ResizableOreFeatureConfig on versions where reflection is blocked by
            // the module system, and any other custom ore feature with a similar shape.
            if (takeover && isOreShaped(configured)) {
                String description = configured.toString().toLowerCase(Locale.ROOT);
                for (ResourceLocation blockId : takeoverBlocks) {
                    if (description.contains(blockId.toString().toLowerCase(Locale.ROOT))) {
                        logRemoval(featureId, blockId, "config text");
                        return true;
                    }
                }
                // If a custom ore feature has no inspectable block ids at all, remove it only
                // when its own id names one of the takeover materials.  This keeps unrelated
                // custom features (for example tree/geode features) alone.
                if (featureId != null && !OresConfig.isExemptNamespace(featureId.getNamespace())
                        && matchesTakeoverMaterial(featureId, takeoverBlocks)) {
                    logRemoval(featureId, null, "feature id");
                    return true;
                }
            }
        }

        // Custom ore features do not necessarily expose an OreConfiguration in a way we can inspect.
        // Only remove when their id clearly points at a whitelisted material; takeover is target-based.
        if (!foundOreConfig && featureId != null && cleanup) {
            if (OresConfig.isExemptNamespace(featureId.getNamespace())) {
                return false;
            }
            if (index.isCleanupFeature(featureId)) {
                logRemoval(featureId, null, "cleanup feature id");
                return true;
            }
        }

        return false;
    }

    private static boolean isOreShaped(ConfiguredFeature<?, ?> configured) {
        Feature<?> feature = configured.feature();
        return feature == Feature.ORE
                || feature == Feature.SCATTERED_ORE
                || feature.getClass().getName().toLowerCase(Locale.ROOT).contains("ore");
    }

    private static boolean matchesTakeoverMaterial(ResourceLocation featureId, Set<ResourceLocation> blocks) {
        String path = featureId.getPath().toLowerCase(Locale.ROOT);
        for (ResourceLocation blockId : blocks) {
            String material = materialOf(blockId);
            if (!material.isEmpty()
                    && (path.equals(material) || path.startsWith(material + "_")
                    || path.endsWith("_" + material) || path.contains("_" + material + "_"))) {
                return true;
            }
        }
        return false;
    }

    private static String materialOf(ResourceLocation blockId) {
        String path = blockId.getPath();
        if (path.startsWith("deepslate_")) {
            path = path.substring("deepslate_".length());
        } else if (path.startsWith("nether_")) {
            path = path.substring("nether_".length());
        } else if (path.startsWith("end_")) {
            path = path.substring("end_".length());
        } else if (path.startsWith("moon_")) {
            path = path.substring("moon_".length());
        } else if (path.startsWith("mars_")) {
            path = path.substring("mars_".length());
        }
        if (path.endsWith("_ore")) {
            path = path.substring(0, path.length() - "_ore".length());
        }
        return path;
    }

    private static void logRemoval(ResourceLocation featureId, ResourceLocation blockId, String reason) {
        if (MekNHOres.LOGGER.isDebugEnabled()) {
            MekNHOres.LOGGER.debug("Removing ore feature {} because of {} ({})",
                    featureId, blockId == null ? "unknown" : blockId, reason);
        }
    }

    /**
     * Vanilla uses {@link OreConfiguration}; some mods (for example Mekanism) use their own record
     * with a {@code targetStates()} accessor.  Reading it reflectively keeps this mod optional and
     * still lets non-element-material ores from those mods survive.
     */
    private static List<OreConfiguration.TargetBlockState> targetStates(FeatureConfiguration config) {
        if (config instanceof OreConfiguration ore) {
            return ore.targetStates;
        }
        try {
            Method method = config.getClass().getMethod("targetStates");
            Object value = method.invoke(config);
            if (value instanceof List<?> list) {
                List<OreConfiguration.TargetBlockState> targets = new ArrayList<>();
                for (Object element : list) {
                    if (element instanceof OreConfiguration.TargetBlockState target) {
                        targets.add(target);
                    }
                }
                return targets;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Not an ore-shaped config; the feature-id fallback below is used instead.
        }
        return List.of();
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
