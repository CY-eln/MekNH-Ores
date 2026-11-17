package c_y.meknh_ores.worldgen;

import com.mojang.serialization.MapCodec;
import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.OresConfig;
import c_y.meknh_ores.resource.UnifiedTagPack;
import c_y.meknh_ores.worldgen.vein.AddVeinBiomeModifier;
import c_y.meknh_ores.worldgen.vein.OreVeinFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Optional;

public final class OreWorldgen {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MekNHOres.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MekNHOres.MODID);

    private OreWorldgen() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register("ore_vein", OreVeinFeature::new);
        FEATURES.register(modEventBus);
        BIOME_MODIFIER_SERIALIZERS.register("remove_foreign_ores",
                () -> RemoveForeignOresBiomeModifier.CODEC);
        BIOME_MODIFIER_SERIALIZERS.register("add_ore_veins",
                () -> AddVeinBiomeModifier.CODEC);
        BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(OreWorldgen::onAddPackFinders);
    }

    private static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !OresConfig.unifyTags()) {
            return;
        }
        event.addRepositorySource(consumer -> {
            PackLocationInfo location = new PackLocationInfo(
                    UnifiedTagPack.PACK_ID,
                    Component.literal("MekNH Ores - Unified Resource Tags"),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );
            Pack.Metadata metadata = new Pack.Metadata(
                    Component.literal("Generated c: ore/ingot tags for MekNH Ores"),
                    PackCompatibility.COMPATIBLE,
                    FeatureFlagSet.of(),
                    List.of(),
                    false
            );
            Pack pack = new Pack(
                    location,
                    new Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo packLocation) {
                            return new UnifiedTagPack(packLocation);
                        }

                        @Override
                        public net.minecraft.server.packs.PackResources openFull(PackLocationInfo packLocation,
                                                                                 Pack.Metadata packMetadata) {
                            return new UnifiedTagPack(packLocation);
                        }
                    },
                    metadata,
                    new PackSelectionConfig(true, Pack.Position.TOP, true)
            );
            consumer.accept(pack);
        });
    }
}
