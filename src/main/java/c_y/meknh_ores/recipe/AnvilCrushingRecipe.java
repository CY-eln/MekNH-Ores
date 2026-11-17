package c_y.meknh_ores.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import c_y.meknh_ores.MekNHOres;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A one-ingredient recipe used by the falling-anvil handler.
 *
 * <p>The recipe itself does not run inside a crafting table.  It is a data-driven description of
 * what a falling anvil can crush; the handler consumes up to {@link #maxCount()} matching item
 * entities and drops the result.</p>
 */
public final class AnvilCrushingRecipe implements Recipe<SingleRecipeInput> {

    public static final RecipeType<AnvilCrushingRecipe> TYPE =
            RecipeType.simple(MekNHOres.rl("anvil_crushing"));

    private final Ingredient ingredient;
    private final ItemStack result;
    private final int maxCount;

    public AnvilCrushingRecipe(Ingredient ingredient, ItemStack result, int maxCount) {
        this.ingredient = ingredient;
        this.result = result.copy();
        this.maxCount = Math.max(1, Math.min(64, maxCount));
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStack result() {
        return result;
    }

    public int maxCount() {
        return maxCount;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, ingredient);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistration.ANVIL_CRUSHING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public static final class Serializer implements RecipeSerializer<AnvilCrushingRecipe> {

        private static final MapCodec<AnvilCrushingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient")
                                .forGetter(AnvilCrushingRecipe::ingredient),
                        ItemStack.STRICT_CODEC.fieldOf("result")
                                .forGetter(AnvilCrushingRecipe::result),
                        Codec.INT.optionalFieldOf("max_count", 4)
                                .forGetter(AnvilCrushingRecipe::maxCount)
                ).apply(instance, AnvilCrushingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AnvilCrushingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<AnvilCrushingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AnvilCrushingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static AnvilCrushingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            int maxCount = buffer.readVarInt();
            return new AnvilCrushingRecipe(ingredient, result, maxCount);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, AnvilCrushingRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.maxCount);
        }
    }
}
