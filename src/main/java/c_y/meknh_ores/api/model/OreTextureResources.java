package c_y.meknh_ores.api.model;

import c_y.meknh_ores.MekNHOres;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * 把 {@code textures/block/ores} 下的底图与 {@code overlay} 下的覆盖层合并。
 *
 * <p>底图：{@code assets/meknh_ores/textures/block/ores/<base>.png}<br>
 * 覆盖层：{@code assets/meknh_ores/textures/block/ores/overlay/<material>.png}<br>
 * 输出：{@code assets/meknh_ores/textures/block/ores/<base>_<material>.png}</p>
 *
 * <p>例如放入 {@code stone.png} 和 {@code overlay/aluminum.png}，会生成
 * {@code stone_aluminum.png}。覆盖层按 alpha 叠加到底图上。</p>
 */
public final class OreTextureResources {

    private OreTextureResources() {
    }

    public static Map<ResourceLocation, byte[]> build() {
        Map<ResourceLocation, byte[]> resources = new LinkedHashMap<>();
        Path ores = oreTextureDirectory();
        if (ores == null) {
            return resources;
        }
        Path overlays = ores.resolve("overlay");
        if (!Files.isDirectory(ores) || !Files.isDirectory(overlays)) {
            return resources;
        }
        try (var baseFiles = Files.list(ores)) {
            Map<String, Path> bases = new java.util.LinkedHashMap<>();
            for (Path base : baseFiles.filter(Files::isRegularFile).filter(OreTextureResources::isPng).toList()) {
                bases.put(stripPng(base.getFileName().toString()), base);
            }
            if (bases.isEmpty()) {
                return resources;
            }
            for (OreTextureDefinition definition : OreTextureRegistry.definitions()) {
                String material = definition.material();
                Path overlayPath = overlays.resolve(material + ".png");
                BufferedImage overlayImage = Files.isRegularFile(overlayPath) ? readImage(overlayPath) : null;
                if (overlayImage == null) {
                    MekNHOres.LOGGER.warn("Ore texture {}: overlay is missing, using base texture only", material);
                }
                var selectedBases = definition.bases().contains("all") ? bases.keySet() : definition.bases();
                for (String baseName : selectedBases) {
                    Path base = bases.get(baseName);
                    if (base == null) {
                        MekNHOres.LOGGER.warn("Ore texture {}: base '{}' is missing, skipped", material, baseName);
                        continue;
                    }
                    BufferedImage baseImage = readImage(base);
                    if (baseImage == null) {
                        continue;
                    }
                    BufferedImage merged = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = merged.createGraphics();
                    try {
                        graphics.drawImage(baseImage, 0, 0, null);
                        if (overlayImage != null) {
                            graphics.drawImage(overlayImage, 0, 0, null);
                        }
                    } finally {
                        graphics.dispose();
                    }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    ImageIO.write(merged, "PNG", output);
                    resources.put(ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID,
                                    "textures/block/ores/" + baseName + "_" + material + ".png"),
                            output.toByteArray());
                }
            }
            MekNHOres.LOGGER.info("Generated {} merged ore texture(s)", resources.size());
        } catch (IOException e) {
            MekNHOres.LOGGER.error("Failed to generate merged ore textures", e);
        }
        return resources;
    }

    private static Path oreTextureDirectory() {
        var fileInfo = ModList.get().getModFileById(MekNHOres.MODID);
        if (fileInfo == null) {
            return null;
        }
        return fileInfo.getFile().getSecureJar().getRootPath()
                .resolve("assets")
                .resolve(MekNHOres.MODID)
                .resolve("textures")
                .resolve("block")
                .resolve("ores");
    }

    private static boolean isPng(Path path) {
        return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png");
    }

    private static BufferedImage readImage(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            return ImageIO.read(input);
        }
    }

    private static String stripPng(String name) {
        return name.substring(0, name.length() - ".png".length());
    }
}
