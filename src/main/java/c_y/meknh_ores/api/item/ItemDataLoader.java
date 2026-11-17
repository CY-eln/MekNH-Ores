package c_y.meknh_ores.api.item;

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
 * 扫描 {@code data/addon/item/*.json} 并批量注册物品。
 */
public final class ItemDataLoader {

    private static final String JSON_SUFFIX = ".json";

    private ItemDataLoader() {
    }

    public static int loadFromMods() {
        int loaded = 0;
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            try {
                loaded += loadFromModFile(modFile);
            } catch (Exception e) {
                MekNHOres.LOGGER.error("Failed to scan item definitions in mod file {}", modFile.getFileName(), e);
            }
        }
        MekNHOres.LOGGER.info("Loaded {} data-driven item definition(s)", loaded);
        return loaded;
    }

    private static int loadFromModFile(IModFile modFile) {
        Path directory = modFile.getSecureJar().getRootPath()
                .resolve("data")
                .resolve(ItemRegistration.ADDON_DATA_DIRECTORY)
                .resolve(ItemRegistration.ITEM_DIRECTORY);
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        int loaded = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (!file.getFileName().toString().endsWith(JSON_SUFFIX)) {
                    continue;
                }
                if (loadItem(file)) {
                    loaded++;
                }
            }
        } catch (IOException e) {
            MekNHOres.LOGGER.error("Failed to scan item definitions in mod file {}", modFile.getFileName(), e);
        }
        return loaded;
    }

    private static boolean loadItem(Path file) {
        String fileName = file.getFileName().toString();
        String path = fileName.substring(0, fileName.length() - JSON_SUFFIX.length());
        ResourceLocation fileId = ResourceLocation.tryBuild(MekNHOres.MODID, path);
        if (fileId == null) {
            MekNHOres.LOGGER.error("Skipping item definition with invalid file name: {} ({})", path, file);
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                MekNHOres.LOGGER.error("Item definition {} must be a JSON object ({})", fileId, file);
                return false;
            }
            JsonObject json = element.getAsJsonObject();
            ItemDefinition definition = ItemDefinition.parse(fileId, json);
            ItemRegistration.register(definition);
            MekNHOres.LOGGER.debug("Loaded data-driven items for {} from {}", definition.tag(), file);
            return true;
        } catch (IOException | RuntimeException e) {
            MekNHOres.LOGGER.error("Failed to load item definition {} from {}", fileId, file, e);
            return false;
        }
    }
}
