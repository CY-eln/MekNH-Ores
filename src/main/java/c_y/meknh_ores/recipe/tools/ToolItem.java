package c_y.meknh_ores.recipe.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A crafted tool used as a durable ingredient by MekNH crafting recipes. */
public final class ToolItem extends Item {

    private final ToolMaterial material;
    private final ToolType type;

    public ToolItem(Properties properties, ToolMaterial material, ToolType type) {
        super(properties);
        this.material = material;
        this.type = type;
    }

    public ToolMaterial material() {
        return material;
    }

    public ToolType type() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.meknh_ores.crafting_tool")
                .withStyle(ChatFormatting.GRAY));
    }
}
