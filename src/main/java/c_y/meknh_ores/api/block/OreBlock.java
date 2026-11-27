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
    private final String material;

    public OreBlock(Properties properties, ResourceLocation rawItemId, String material) {
        super(properties);
        this.rawItemId = rawItemId;
        this.material = material;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack drop = resolveDrop();
        return drop.isEmpty() ? List.of() : List.of(drop);
    }

    /**
     * Prefer the explicitly configured raw item, then the same {@code raw_<material>} id in the
     * usual ore namespaces.  If no raw item exists (for example a custom ore whose material only has
     * ingots/plates), fall back to the ore block item so breaking the block never yields nothing.
     */
    private ItemStack resolveDrop() {
        Item raw = BuiltInRegistries.ITEM.get(this.rawItemId);
        if (raw != Items.AIR) {
            return new ItemStack(raw);
        }

        for (String namespace : List.of("meknh_ores", "minecraft", "mekanism", "ad_astra")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, "raw_" + this.material);
            raw = BuiltInRegistries.ITEM.get(id);
            if (raw != Items.AIR) {
                return new ItemStack(raw);
            }
        }

        Item self = this.asItem();
        return self == Items.AIR ? ItemStack.EMPTY : new ItemStack(self);
    }
}
