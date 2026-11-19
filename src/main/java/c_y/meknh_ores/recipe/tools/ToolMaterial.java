package c_y.meknh_ores.recipe.tools;

/**
 * Tool materials and the GregTech colors used to tint their textures.
 *
 * <p>Colors and durability are taken from GregTech's Iron and Steel material definitions:
 * Iron {@code 0xEEEEEE / 0x979797}, Steel {@code 0xA7A7A7 / 0x121C37}.</p>
 */
public enum ToolMaterial {
    IRON("iron", 0xEEEEEE, 0x979797, 256),
    STEEL("steel", 0xA7A7A7, 0x121C37, 512);

    private final String id;
    private final int primaryColor;
    private final int secondaryColor;
    private final int durability;

    ToolMaterial(String id, int primaryColor, int secondaryColor, int durability) {
        this.id = id;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.durability = durability;
    }

    public String id() {
        return id;
    }

    public int primaryColor() {
        return primaryColor;
    }

    public int secondaryColor() {
        return secondaryColor;
    }

    public int durability() {
        return durability;
    }
}
