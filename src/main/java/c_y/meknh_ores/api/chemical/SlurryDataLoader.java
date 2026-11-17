package c_y.meknh_ores.api.chemical;

import c_y.meknh_ores.MekNHOres;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;

/**
 * 扫描 {@code data/addon/slurry/*.json} 并注册 dirty/clean Slurry。
 */
public final class SlurryDataLoader {

    private static final String JSON_SUFFIX = ".json";

    private SlurryDataLoader() {
    }

    public static int loadFromMods() {
        int loaded = 0;
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            try {
                loaded += loadFromModFile(modFile);
            } catch (Exception e) {
                MekNHOres.LOGGER.error("Failed to scan slurry definitions in mod file {}", modFile.getFileName(), e);
            }
        }
        MekNHOres.LOGGER.info("Loaded {} data-driven slurry definition(s)", loaded);
        return loaded;
    }

    private static int loadFromModFile(IModFile modFile) {
        Path directory = modFile.getSecureJar().getRootPath()
                .resolve("data")
                .resolve("addon")
                .resolve("slurry");
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        int loaded = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (!file.getFileName().toString().endsWith(JSON_SUFFIX)) {
                    continue;
                }
                if (loadSlurry(file)) {
                    loaded++;
                }
            }
        } catch (IOException e) {
            MekNHOres.LOGGER.error("Failed to scan slurry definitions in mod file {}", modFile.getFileName(), e);
        }
        return loaded;
    }

    private static boolean loadSlurry(Path file) {
        String fileName = file.getFileName().toString();
        String path = fileName.substring(0, fileName.length() - JSON_SUFFIX.length());
        ResourceLocation fileId = ResourceLocation.tryBuild(MekNHOres.MODID, path);
        if (fileId == null) {
            MekNHOres.LOGGER.error("Skipping slurry definition with invalid file name: {} ({})", path, file);
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                MekNHOres.LOGGER.error("Slurry definition {} must be a JSON object ({})", fileId, file);
                return false;
            }
            JsonObject json = element.getAsJsonObject();
            SlurryDefinition definition = SlurryDefinition.parse(fileId, json);
            SlurryRegistration.register(definition);
            MekNHOres.LOGGER.debug("Loaded slurry {} from {}", definition.id(), file);
            return true;
        } catch (IOException | RuntimeException e) {
            MekNHOres.LOGGER.error("Failed to load slurry definition {} from {}", fileId, file, e);
            return false;
        }
    }
}
