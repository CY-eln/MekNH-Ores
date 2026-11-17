package c_y.meknh_ores.worldgen.vein;

import com.mojang.serialization.MapCodec;
import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.OresConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;

/**
 * Adds the single vein feature to every biome.  The placed-feature holder is captured during
 * {@code ServerAboutToStartEvent}, before biome modifiers run, so this also works on an integrated
 * (single-player) server where {@link ServerLifecycleHooks#getCurrentServer()} may still be null.
 */
public final class AddVeinBiomeModifier implements BiomeModifier {
    public static final AddVeinBiomeModifier INSTANCE = new AddVeinBiomeModifier();
    public static final MapCodec<AddVeinBiomeModifier> CODEC = MapCodec.unit(INSTANCE);
    public static final ResourceKey<PlacedFeature> PLACED_FEATURE = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID, "ore_vein")
    );

    @Nullable
    private static Holder<PlacedFeature> cachedFeature;
    private static boolean warned;
    private static boolean logged;

    private AddVeinBiomeModifier() {
    }

    /** Called from ServerAboutToStartEvent, after datapack registries are loaded. */
    public static void prepare(MinecraftServer server) {
        cachedFeature = server.registryAccess()
                .registryOrThrow(Registries.PLACED_FEATURE)
                .getHolderOrThrow(PLACED_FEATURE);
        warned = false;
        logged = false;
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !OresConfig.enableVeinTakeover()) {
            return;
        }

        Holder<PlacedFeature> feature = cachedFeature;
        if (feature == null) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                feature = server.registryAccess()
                        .registryOrThrow(Registries.PLACED_FEATURE)
                        .getHolderOrThrow(PLACED_FEATURE);
                cachedFeature = feature;
            }
        }
        if (feature == null) {
            if (!warned) {
                warned = true;
                MekNHOres.LOGGER.warn("Ore vein feature was not prepared before biome modification; skipping");
            }
            return;
        }

        try {
            // Run in UNDERGROUND_DECORATION, i.e. after vanilla/Mekanism/Ad Astra UNDERGROUND_ORES
            // features.  The feature clears original ore blocks in the chunk and then places the
            // GT veins; putting it in UNDERGROUND_ORES lets the sorter run it before some vanilla
            // ore features and leaves those original ores behind.
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, feature);
            if (!logged) {
                logged = true;
                MekNHOres.LOGGER.info("Added ore vein feature to biome {}", biome.unwrapKey()
                        .map(key -> key.location().toString()).orElse("unknown"));
            }
        } catch (Exception exception) {
            if (!warned) {
                warned = true;
                MekNHOres.LOGGER.error("Unable to add the ore vein feature; check meknh_ores placed_feature JSON", exception);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
