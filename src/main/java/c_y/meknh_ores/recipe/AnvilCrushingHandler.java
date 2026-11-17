package c_y.meknh_ores.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Turns copper ingots lying under a falling anvil into copper plates.
 *
 * <p>NeoForge has no falling-block landing event, so this listens to the post-tick of the falling
 * anvil.  By that point vanilla has placed the anvil block and discarded the entity.</p>
 */
public final class AnvilCrushingHandler {

    private static final int MAX_PER_IMPACT = 4;
    private static final Set<UUID> PROCESSED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private AnvilCrushingHandler() {
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof FallingBlockEntity falling)) {
            return;
        }

        Level level = falling.level();
        if (level.isClientSide) {
            return;
        }
        if (!falling.getBlockState().is(BlockTags.ANVIL)) {
            return;
        }
        if (!falling.isRemoved() && !falling.onGround()) {
            return;
        }

        BlockPos pos = falling.blockPosition();
        if (!level.getBlockState(pos).is(BlockTags.ANVIL)) {
            return;
        }
        if (!PROCESSED.add(falling.getUUID())) {
            return;
        }

        crush(level, pos);
    }

    private static void crush(Level level, BlockPos pos) {
        AABB area = new AABB(
                pos.getX() - 0.25, pos.getY() - 1.0, pos.getZ() - 0.25,
                pos.getX() + 1.25, pos.getY() + 1.0, pos.getZ() + 1.25
        );
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                item -> !item.isRemoved() && !item.getItem().isEmpty());
        items.sort(Comparator.comparingDouble(item -> item.distanceToSqr(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));

        int remaining = MAX_PER_IMPACT;
        for (ItemEntity item : items) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = item.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            Optional<RecipeHolder<AnvilCrushingRecipe>> match = level.getRecipeManager()
                    .getRecipeFor(AnvilCrushingRecipe.TYPE, new SingleRecipeInput(stack), level);
            if (match.isEmpty()) {
                continue;
            }

            AnvilCrushingRecipe recipe = match.get().value();
            int take = Math.min(remaining, Math.min(stack.getCount(), recipe.maxCount()));
            if (take <= 0) {
                continue;
            }

            stack.shrink(take);
            if (stack.isEmpty()) {
                item.discard();
            }

            ItemStack output = recipe.result().copyWithCount(recipe.result().getCount() * take);
            Containers.dropItemStack(level,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, output);
            remaining -= take;
        }
    }
}
