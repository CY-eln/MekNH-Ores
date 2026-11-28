package c_y.meknh_ores.compat.jei;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.OresConfig;
import c_y.meknh_ores.recipe.AnvilCrushingRecipe;
import c_y.meknh_ores.resource.UnifiedResourceIndex;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JEI side of the unification: after JEI has received its ingredient list, hide foreign ores and
 * ingots (and optionally raw ores, nuggets, dusts and storage blocks).  Exempt namespaces are never
 * touched, and the operation is idempotent because JEI may rebuild its runtime after a reload.
 */
@JeiPlugin
public final class MekNHOresJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID, "jei_plugin");
    private static IJeiRuntime runtime;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new AnvilCrushingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<AnvilCrushingRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(AnvilCrushingRecipe.TYPE)
                .stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(AnvilCrushingJeiCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.ANVIL), AnvilCrushingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CHIPPED_ANVIL), AnvilCrushingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(Blocks.DAMAGED_ANVIL), AnvilCrushingJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        MekNHOresJeiPlugin.runtime = runtime;
        hideAll();
    }

    /** Called again after the server sends its cleanup whitelist. */
    public static void reapply() {
        if (runtime != null) {
            hideAll();
        }
    }

    private static void hideAll() {
        if (runtime == null) {
            return;
        }
        UnifiedResourceIndex index = UnifiedResourceIndex.get();
        List<ItemStack> hidden = runtime.getIngredientManager().getAllItemStacks().stream()
                .filter(index::shouldHide)
                .toList();
        if (hidden.isEmpty()) {
            return;
        }

        runtime.getJeiHelpers().getIngredientVisibility().hideIngredients(
                VanillaTypes.ITEM_STACK,
                hidden,
                List.of(UidContext.Ingredient, UidContext.Recipe)
        );
        if (OresConfig.logHiddenItems()) {
            MekNHOres.LOGGER.info("Hid {} foreign ore/ingot stack(s) from JEI", hidden.size());
        }
    }
}
