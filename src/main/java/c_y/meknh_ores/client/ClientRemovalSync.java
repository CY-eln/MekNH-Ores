package c_y.meknh_ores.client;

/**
 * Client-only bridge kept in a separate class so the dedicated server never loads JEI classes.
 */
public final class ClientRemovalSync {
    private ClientRemovalSync() {
    }

    public static void reapplyJei() {
        try {
            Class.forName("c_y.meknh_ores.compat.jei.MekNHOresJeiPlugin")
                    .getMethod("reapply")
                    .invoke(null);
        } catch (Throwable ignored) {
            // JEI is optional.
        }
    }
}
