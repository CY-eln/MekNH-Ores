package c_y.meknh_ores.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 数据驱动物品：显示名由材料名 + 形态名组成。
 */
public final class DataItem extends Item {

    private final ItemDefinition definition;
    private final ItemForm form;

    public DataItem(Properties properties, ItemDefinition definition, ItemForm form) {
        super(properties);
        this.definition = definition;
        this.form = form;
    }

    public ItemDefinition definition() {
        return this.definition;
    }

    public ItemForm form() {
        return this.form;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.form.nameKey(),
                Component.literal(this.definition.name()),
                Component.translatable(this.form.translationKey()));
    }
}
