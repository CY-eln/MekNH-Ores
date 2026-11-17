package c_y.meknh_ores.api.item;

import c_y.meknh_ores.MekNHOres;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * 为数据驱动物品生成 {@code models/item/<id>.json}。
 *
 * <p>所有形态共用 {@code minecraft:item/generated}，第 0 层直接指向
 * {@code textures/item/<形态>/<材料>.png} 成品贴图，不做灰度染色。</p>
 */
public final class ItemModelResources {

    private ItemModelResources() {
    }

    public static Map<ResourceLocation, byte[]> build() {
        Map<ResourceLocation, byte[]> resources = new LinkedHashMap<>();
        for (ItemRegistration.ItemVariant variant : ItemRegistration.variants()) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", MekNHOres.MODID + ":item/"
                    + variant.form().tag() + "/" + variant.definition().material());
            model.add("textures", textures);
            resources.put(ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID,
                            "models/item/" + variant.id().getPath() + ".json"),
                    model.toString().getBytes(StandardCharsets.UTF_8));
        }
        return resources;
    }
}
