package c_y.meknh_ores.api.block;

import c_y.meknh_ores.MekNHOres;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * 生成矿石/粗矿块的 blockstate、方块模型和物品模型。
 */
public final class OreBlockModelResources {

    private OreBlockModelResources() {
    }

    public static Map<ResourceLocation, byte[]> build() {
        Map<ResourceLocation, byte[]> resources = new LinkedHashMap<>();
        for (OreBlockRegistration.OreVariant variant : OreBlockRegistration.variants()) {
            String path = variant.path();
            String model = MekNHOres.MODID + ":block/ore/" + path;
            resources.put(MekNHOres.rl("blockstates/" + path + ".json"), blockstate(model));
            resources.put(MekNHOres.rl("models/block/ore/" + path + ".json"),
                    cubeAll(MekNHOres.MODID + ":block/ores/" + variant.texture()));
            resources.put(MekNHOres.rl("models/item/" + path + ".json"), parent(model));
        }
        for (OreBlockRegistration.RawVariant variant : OreBlockRegistration.rawVariants()) {
            String path = variant.path();
            String model = MekNHOres.MODID + ":block/raw/" + variant.material();
            resources.put(MekNHOres.rl("blockstates/" + path + ".json"), blockstate(model));
            resources.put(MekNHOres.rl("models/block/raw/" + variant.material() + ".json"),
                    cubeAll(MekNHOres.MODID + ":block/raw/" + variant.material()));
            resources.put(MekNHOres.rl("models/item/" + path + ".json"), parent(model));
        }
        return resources;
    }

    private static byte[] blockstate(String model) {
        JsonObject json = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        variants.add("", variant);
        json.add("variants", variants);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] cubeAll(String texture) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", texture);
        json.add("textures", textures);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] parent(String model) {
        JsonObject json = new JsonObject();
        json.addProperty("parent", model);
        return json.toString().getBytes(StandardCharsets.UTF_8);
    }
}
