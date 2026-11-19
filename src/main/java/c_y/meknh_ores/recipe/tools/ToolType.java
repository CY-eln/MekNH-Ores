package c_y.meknh_ores.recipe.tools;

/** The tool families currently implemented by MekNH Ores. */
public enum ToolType {
    FILE("file"),
    MORTAR("mortar");

    private final String id;

    ToolType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
