package c_y.meknh_ores.api.block;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.api.model.OreTextureRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 数据驱动矿石/粗矿块注册。只有同时存在覆盖层与底图的材料才会注册。
 */
public final class OreBlockRegistration {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MekNHOres.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekNHOres.MODID);
    private static final Map<String, OreVariant> VARIANTS = new LinkedHashMap<>();
    private static final List<RawVariant> RAW_VARIANTS = new ArrayList<>();

    private OreBlockRegistration() {
    }

    public static synchronized void initialize(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(OreBlockRegistration::addCreativeTab);
    }

    public static synchronized void registerAll() {
        for (var definition : OreTextureRegistry.definitions()) {
            String material = definition.material();
            for (String base : definition.bases()) {
                if (!assetExists("textures/block/ores/" + base + ".png")) {
                    continue;
                }
                String path = blockPath(base, material);
                String texture = base + "_" + material;
                ResourceLocation drop = definition.drop() != null
                        ? definition.drop()
                        : MekNHOres.rl("raw_" + material);
                registerOre(path, material, base, texture, drop);
            }
            if (assetExists("textures/block/raw/" + material + ".png")
                    && assetExists("textures/item/raw/" + material + ".png")) {
                registerRaw(material);
            }
        }
        MekNHOres.LOGGER.info("Registered {} ore block(s) and {} raw block(s)",
                VARIANTS.size(), RAW_VARIANTS.size());
    }

    private static void registerOre(String path, String material, String base, String texture, ResourceLocation drop) {
        if (VARIANTS.containsKey(path)) {
            return;
        }
        DeferredBlock<OreBlock> block = BLOCKS.register(path, () -> new OreBlock(
                BlockBehaviour.Properties.of()
                        .strength(3.0F, 3.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE),
                drop,
                material));
        DeferredItem<BlockItem> item = ITEMS.registerItem(path,
                properties -> new BlockItem(block.get(), properties));
        VARIANTS.put(path, new OreVariant(path, material, base, texture, block, item));
    }

    private static void registerRaw(String material) {
        String path = "raw_" + material + "_block";
        if (RAW_VARIANTS.stream().anyMatch(variant -> variant.path().equals(path))) {
            return;
        }
        DeferredBlock<RawBlock> block = BLOCKS.register(path, () -> new RawBlock(
                BlockBehaviour.Properties.of()
                        .strength(4.0F, 4.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE)));
        DeferredItem<BlockItem> item = ITEMS.registerItem(path,
                properties -> new BlockItem(block.get(), properties));
        RAW_VARIANTS.add(new RawVariant(path, material, block, item));
    }

    private static String blockPath(String base, String material) {
        return switch (base) {
            case "stone" -> material + "_ore";
            case "deepslate" -> "deepslate_" + material + "_ore";
            case "netherrack" -> "nether_" + material + "_ore";
            case "end_stone" -> "end_" + material + "_ore";
            case "moon_stone" -> "moon_" + material + "_ore";
            case "mars_stone" -> "mars_" + material + "_ore";
            default -> base + "_" + material + "_ore";
        };
    }

    private static boolean assetExists(String path) {
        var fileInfo = net.neoforged.fml.ModList.get().getModFileById(MekNHOres.MODID);
        if (fileInfo == null) {
            return false;
        }
        Path file = fileInfo.getFile().getSecureJar().getRootPath()
                .resolve("assets")
                .resolve(MekNHOres.MODID)
                .resolve(path);
        return Files.isRegularFile(file);
    }

    private static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            for (OreVariant variant : VARIANTS.values()) {
                event.accept(variant.item().get());
            }
            for (RawVariant variant : RAW_VARIANTS) {
                event.accept(variant.item().get());
            }
        }
    }

    public static synchronized List<OreVariant> variants() {
        return List.copyOf(VARIANTS.values());
    }

    public static synchronized List<RawVariant> rawVariants() {
        return List.copyOf(RAW_VARIANTS);
    }

    public record OreVariant(String path, String material, String base, String texture,
                             DeferredBlock<OreBlock> block, DeferredItem<BlockItem> item) {
        public ResourceLocation id() {
            return item.getId();
        }
    }

    public record RawVariant(String path, String material,
                             DeferredBlock<RawBlock> block, DeferredItem<BlockItem> item) {
        public ResourceLocation id() {
            return item.getId();
        }
    }
}
