package c_y.meknh_ores.recipe;

import c_y.meknh_ores.MekNHOres;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Recipe serializers used by MekNH Ores. */
public final class RecipeRegistration {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MekNHOres.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, DurabilityShapelessRecipe.Serializer>
            DURABILITY_SHAPELESS = SERIALIZERS.register(
            "durability_shapeless", DurabilityShapelessRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, AnvilCrushingRecipe.Serializer>
            ANVIL_CRUSHING = SERIALIZERS.register(
            "anvil_crushing", AnvilCrushingRecipe.Serializer::new);

    private RecipeRegistration() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
