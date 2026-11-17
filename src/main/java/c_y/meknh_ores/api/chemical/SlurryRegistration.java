package c_y.meknh_ores.api.chemical;

import c_y.meknh_ores.MekNHOres;
import java.util.LinkedHashMap;
import java.util.Map;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import mekanism.common.registration.impl.ChemicalDeferredRegister;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

/**
 * 数据驱动 Slurry 注册：每个材料注册 dirty/clean 两个 Mekanism 化学品，共用 Mekanism 的
 * {@code mekanism:slurry/dirty} / {@code slurry/clean} 动态贴图，只按材料 tint 着色。
 */
public final class SlurryRegistration {

    private static final Map<ResourceLocation, SlurryDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static ChemicalDeferredRegister register;

    private SlurryRegistration() {
    }

    public static synchronized void initialize(IEventBus bus) {
        if (register != null) {
            throw new IllegalStateException("SlurryRegistration has already been initialized");
        }
        register = new ChemicalDeferredRegister(MekNHOres.MODID);
        register.register(bus);
    }

    public static synchronized void register(SlurryDefinition definition) {
        if (register == null) {
            throw new IllegalStateException("SlurryRegistration has not been initialized");
        }
        if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Slurry " + definition.id() + " has already been registered");
        }
        String base = definition.id().getPath();
        register.register("dirty_" + base,
                () -> new Chemical(ChemicalBuilder.dirtySlurry().tint(definition.tint())));
        register.register("clean_" + base,
                () -> new Chemical(ChemicalBuilder.cleanSlurry().tint(definition.tint())));
    }

    public static synchronized Map<ResourceLocation, SlurryDefinition> definitions() {
        return Map.copyOf(DEFINITIONS);
    }
}
