package c_y.meknh_ores.api.block;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * 数据驱动矿石方块：破坏时掉落对应的粗矿物品。
 */
public class OreBlock extends Block {

    private final ResourceLocation rawItemId;

    public OreBlock(Properties properties, ResourceLocation rawItemId) {
        super(properties);
        this.rawItemId = rawItemId;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Item raw = BuiltInRegistries.ITEM.get(this.rawItemId);
        return raw == null || raw == Items.AIR ? List.of() : List.of(new ItemStack(raw));
    }
}
