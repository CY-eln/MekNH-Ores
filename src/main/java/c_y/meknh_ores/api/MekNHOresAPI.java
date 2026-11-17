package c_y.meknh_ores.api;

import c_y.meknh_ores.OresConfig;
import c_y.meknh_ores.resource.ResourceKind;
import c_y.meknh_ores.resource.UnifiedResourceIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Read-only API for packs and add-ons that want to know which item MekNH Ores considers canonical.
 */
public final class MekNHOresAPI {
    private MekNHOresAPI() {
    }

    public static boolean isExempt(ResourceLocation id) {
        return OresConfig.isExemptItem(id);
    }

    public static boolean isUnifiedResource(ItemStack stack) {
        return UnifiedResourceIndex.get().isRecognised(stack);
    }

    public static Optional<ItemStack> canonical(ItemStack stack) {
        return UnifiedResourceIndex.get().canonical(stack);
    }

    public static List<ItemStack> variants(ItemStack stack) {
        return UnifiedResourceIndex.get().variants(stack);
    }

    public static Optional<UnifiedResourceIndex.UnifiedResource> resource(ResourceKind kind, String material) {
        return UnifiedResourceIndex.get().resource(kind, material);
    }
}
