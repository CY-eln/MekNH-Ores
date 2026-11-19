package c_y.meknh_ores;

import c_y.meknh_ores.api.block.OreBlockRegistration;
import c_y.meknh_ores.api.chemical.SlurryDataLoader;
import c_y.meknh_ores.api.chemical.SlurryRegistration;
import c_y.meknh_ores.api.item.ItemDataLoader;
import c_y.meknh_ores.api.item.ItemRegistration;
import c_y.meknh_ores.api.model.OreTextureRegistry;
import c_y.meknh_ores.data.RemovalData;
import c_y.meknh_ores.network.RemovalNetwork;
import c_y.meknh_ores.recipe.AnvilCrushingHandler;
import c_y.meknh_ores.recipe.RecipeRegistration;
import c_y.meknh_ores.recipe.tools.ToolRegistration;
import c_y.meknh_ores.resource.UnifiedResourceIndex;
import c_y.meknh_ores.worldgen.OreWorldgen;
import c_y.meknh_ores.worldgen.vein.AddVeinBiomeModifier;
import c_y.meknh_ores.worldgen.vein.VeinData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MekNH Ores
 *
 * <p>A small, pack-oriented unification layer for ores and ingots.  It has four jobs:</p>
 * <ol>
 *     <li>Publish a generated set of conventional {@code c:ores/*}, {@code c:ingots/*} (and related)
 *     tags for every item/block that follows a conventional resource name.  This is the actual
 *     "ore dictionary" part and works for every installed mod, without hard-coding a mod list.</li>
 *     <li>Take over world generation for ores explicitly used in KubeJS-defined GregTech-style veins.</li>
 *     <li>Remove world-generation features only for whitelisted ores; everything else is kept.</li>
 *     <li>Hide duplicate ores/ingots from JEI only for materials on the cleanup whitelist, so
 *     fantasy metals and alloys are never touched accidentally.</li>
 * </ol>
 */
@Mod(MekNHOres.MODID)
public final class MekNHOres {
    public static final String MODID = "meknh_ores";
    public static final Logger LOGGER = LoggerFactory.getLogger("MekNH Ores");

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public MekNHOres(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, OresConfig.SPEC);
        SlurryRegistration.initialize(modEventBus);
        SlurryDataLoader.loadFromMods();
        OreTextureRegistry.loadFromMods();
        ItemRegistration.initialize(modEventBus);
        ItemDataLoader.loadFromMods();
        OreBlockRegistration.initialize(modEventBus);
        OreBlockRegistration.registerAll();
        ToolRegistration.register(modEventBus);
        RecipeRegistration.register(modEventBus);
        OreWorldgen.register(modEventBus);
        modEventBus.addListener(RemovalNetwork::register);
        NeoForge.EVENT_BUS.addListener(RemovalNetwork::onLogin);
        NeoForge.EVENT_BUS.addListener(AnvilCrushingHandler::onEntityTick);
        NeoForge.EVENT_BUS.addListener(UnifiedResourceIndex::onTagsUpdated);
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent event) -> {
            AddVeinBiomeModifier.prepare(event.getServer());
            RemovalData.reload(event.getServer().getResourceManager());
            UnifiedResourceIndex.invalidate();
            UnifiedResourceIndex.get();
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            // Rebuild the feature-sort index after every biome modifier has run.  NeoForge does
            // this in ServerLifecycleHooks, but doing it here as well is harmless and protects
            // against another mod invalidating/rebuilding the index later in startup.
            event.getServer().getAllLevels().forEach(level ->
                    level.getChunkSource().getGenerator().refreshFeaturesPerStep());
            UnifiedResourceIndex index = UnifiedResourceIndex.get();
            LOGGER.info("Unified resource index ready: {} families across {} items",
                    index.familyCount(), index.recognisedItemCount());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> VeinData.clearCache());
    }
}
