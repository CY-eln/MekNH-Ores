package c_y.meknh_ores.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/**
 * Registered through {@code kubejs.plugins.txt}.  Exposes {@code MekNHOres} to server scripts.
 */
public final class MekNHOresKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry event) {
        event.add("MekNHOres", new MekNHOresKubeJS());
    }
}
