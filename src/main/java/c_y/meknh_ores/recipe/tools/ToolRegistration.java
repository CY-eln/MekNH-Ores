package c_y.meknh_ores.recipe.tools;

import c_y.meknh_ores.MekNHOres;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers the file and mortar items for every supported material. */
public final class ToolRegistration {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MekNHOres.MODID);

    private static final Map<ResourceLocation, DeferredItem<Item>> TOOLS = new LinkedHashMap<>();

    static {
        for (ToolMaterial material : ToolMaterial.values()) {
            for (ToolType type : ToolType.values()) {
                String path = type.id() + "_" + material.id();
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID, path);
                DeferredItem<Item> item = ITEMS.registerItem(path,
                        properties -> new ToolItem(
                                properties
                                        .durability(material.durability())
                                        .stacksTo(1),
                                material,
                                type));
                TOOLS.put(id, item);
            }
        }
    }

    private ToolRegistration() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ToolRegistration::addCreativeTab);
    }

    public static Map<ResourceLocation, DeferredItem<Item>> tools() {
        return Map.copyOf(TOOLS);
    }

    private static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (DeferredItem<Item> item : TOOLS.values()) {
                event.accept(item.get());
            }
        }
    }
}
