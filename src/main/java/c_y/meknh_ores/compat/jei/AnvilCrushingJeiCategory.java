package c_y.meknh_ores.compat.jei;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.recipe.AnvilCrushingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** JEI category for falling-anvil crushing recipes. */
public final class AnvilCrushingJeiCategory implements IRecipeCategory<AnvilCrushingRecipe> {

    public static final RecipeType<AnvilCrushingRecipe> RECIPE_TYPE =
            new RecipeType<>(MekNHOres.rl("anvil_crushing"), AnvilCrushingRecipe.class);

    private final IGuiHelper guiHelper;
    private final IDrawable icon;

    public AnvilCrushingJeiCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Blocks.ANVIL));
    }

    @Override
    public RecipeType<AnvilCrushingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.meknh_ores.anvil_crushing");
    }

    @Override
    public IDrawable getBackground() {
        return this.guiHelper.createBlankDrawable(120, 40);
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilCrushingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 11).addIngredients(recipe.ingredient());
        builder.addSlot(RecipeIngredientRole.CATALYST, 48, 11)
                .addItemStack(new ItemStack(Blocks.ANVIL));
        builder.addOutputSlot(88, 11).addItemStack(recipe.result());
    }

    @Override
    public void draw(AnvilCrushingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.drawString(Minecraft.getInstance().font,
                Component.translatable("jei.meknh_ores.anvil_crushing.max", recipe.maxCount()),
                8, 28, 0x404040, false);
    }
}
