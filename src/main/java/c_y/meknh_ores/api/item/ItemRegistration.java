package c_y.meknh_ores.api.item;

import c_y.meknh_ores.MekNHOres;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * 数据驱动物品的批量注册入口。
 *
 * <p>每个材料 JSON 会按形态生成多个物品，注册名为 {@code <形态>_<材料>}，例如
 * {@code ingot_aluminum}、{@code dust_aluminum}。</p>
 */
public final class ItemRegistration {

    public static final String ADDON_DATA_DIRECTORY = "addon";
    public static final String ITEM_DIRECTORY = "item";

    private static final Map<ResourceLocation, ItemVariant> VARIANTS = new LinkedHashMap<>();
    private static DeferredRegister.Items items;

    private ItemRegistration() {
    }

    public static synchronized void initialize(IEventBus bus) {
        if (items != null) {
            throw new IllegalStateException("ItemRegistration has already been initialized");
        }
        items = DeferredRegister.createItems(MekNHOres.MODID);
        items.register(bus);
        bus.addListener(ItemRegistration::addCreativeTab);
    }

    public static synchronized void register(ItemDefinition definition) {
        if (items == null) {
            throw new IllegalStateException("ItemRegistration has not been initialized");
        }
        for (ItemForm form : definition.forms()) {
            String path = definition.itemPath(form);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID, path);
            if (VARIANTS.containsKey(id)) {
                throw new IllegalStateException("Item " + id + " has already been registered");
            }
            DeferredItem<Item> item = items.registerItem(path,
                    properties -> new DataItem(properties, definition, form));
            VARIANTS.put(id, new ItemVariant(item, definition, form));
        }
    }

    private static void addCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (ItemVariant variant : VARIANTS.values()) {
                event.accept(variant.item().get());
            }
        }
    }

    public static synchronized Collection<ItemVariant> variants() {
        return java.util.List.copyOf(VARIANTS.values());
    }

    @Nullable
    public static synchronized ItemVariant get(ResourceLocation id) {
        return VARIANTS.get(id);
    }

    public record ItemVariant(DeferredItem<Item> item, ItemDefinition definition, ItemForm form) {
        public ResourceLocation id() {
            return this.item.getId();
        }
    }
}
