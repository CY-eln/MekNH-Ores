package c_y.meknh_ores.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.List;

/**
 * A shapeless crafting recipe that keeps damageable ingredients in the crafting grid and
 * returns them with one point of durability consumed instead of consuming the whole item.
 *
 * <p>Vanilla {@link ShapelessRecipe} always consumes every ingredient.  This subclass only
 * changes {@link #getRemainingItems(CraftingInput)}; matching, result and ingredient layout are
 * inherited unchanged, so JEI and the vanilla recipe book still see a normal shapeless recipe.</p>
 */
public final class DurabilityShapelessRecipe extends ShapelessRecipe {

    private final ItemStack result;

    public DurabilityShapelessRecipe(String group, CraftingBookCategory category, ItemStack result,
                                     List<Ingredient> ingredients) {
        this(group, category, result,
                NonNullList.of(Ingredient.EMPTY, ingredients.toArray(Ingredient[]::new)));
    }

    private DurabilityShapelessRecipe(String group, CraftingBookCategory category, ItemStack result,
                                      NonNullList<Ingredient> ingredients) {
        super(group, category, result, ingredients);
        this.result = result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistration.DURABILITY_SHAPELESS.get();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }

            ItemStack damaged = stack.copyWithCount(1);
            int newDamage = damaged.getDamageValue() + 1;
            if (newDamage > damaged.getMaxDamage()) {
                // The tool would break on this use: consume it as usual.
                continue;
            }
            damaged.setDamageValue(newDamage);
            remaining.set(i, damaged);
        }
        return remaining;
    }

    public static final class Serializer implements RecipeSerializer<DurabilityShapelessRecipe> {

        private static final MapCodec<DurabilityShapelessRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
                        CraftingBookCategory.CODEC.fieldOf("category")
                                .orElse(CraftingBookCategory.MISC)
                                .forGetter(ShapelessRecipe::category),
                        ItemStack.STRICT_CODEC.fieldOf("result")
                                .forGetter(recipe -> recipe.result),
                        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                                .forGetter(ShapelessRecipe::getIngredients)
                ).apply(instance, DurabilityShapelessRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, DurabilityShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<DurabilityShapelessRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DurabilityShapelessRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static DurabilityShapelessRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            int size = buffer.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            return new DurabilityShapelessRecipe(group, category, result, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, DurabilityShapelessRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            buffer.writeVarInt(recipe.getIngredients().size());
            for (Ingredient ingredient : recipe.getIngredients()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
        }
    }
}
