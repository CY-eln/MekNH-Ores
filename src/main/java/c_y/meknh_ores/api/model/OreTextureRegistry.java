package c_y.meknh_ores.api.model;

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
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.Nullable;

/**
 * 读取 {@code data/addon/block/ore/*.json}，控制每种矿石使用哪些底图。
 */
public final class OreTextureRegistry {

    private static final Map<String, OreTextureDefinition> DEFINITIONS = new LinkedHashMap<>();

    private OreTextureRegistry() {
    }

    public static int loadFromMods() {
        int loaded = 0;
        for (IModFileInfo modFileInfo : ModList.get().getModFiles()) {
            IModFile modFile = modFileInfo.getFile();
            try {
                loaded += loadFromModFile(modFile);
            } catch (Exception e) {
                MekNHOres.LOGGER.error("Failed to scan ore texture definitions in mod file {}", modFile.getFileName(), e);
            }
        }
        MekNHOres.LOGGER.info("Loaded {} ore texture definition(s)", loaded);
        return loaded;
    }

    private static int loadFromModFile(IModFile modFile) {
        Path directory = modFile.getSecureJar().getRootPath()
                .resolve("data")
                .resolve("addon")
                .resolve("block")
                .resolve("ore");
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        int loaded = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
            for (Path file : files) {
                if (!file.getFileName().toString().endsWith(".json")) {
                    continue;
                }
                if (load(file)) {
                    loaded++;
                }
            }
        } catch (IOException e) {
            MekNHOres.LOGGER.error("Failed to scan ore texture definitions in mod file {}", modFile.getFileName(), e);
        }
        return loaded;
    }

    private static boolean load(Path file) {
        String fileName = file.getFileName().toString();
        String path = fileName.substring(0, fileName.length() - ".json".length());
        ResourceLocation fileId = ResourceLocation.tryBuild(MekNHOres.MODID, path);
        if (fileId == null) {
            return false;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                MekNHOres.LOGGER.error("Ore texture definition {} must be a JSON object ({})", fileId, file);
                return false;
            }
            OreTextureDefinition definition = OreTextureDefinition.parse(fileId, element.getAsJsonObject());
            OreTextureDefinition previous = DEFINITIONS.putIfAbsent(definition.material(), definition);
            if (previous != null) {
                throw new IllegalStateException("Ore texture material " + definition.material() + " already has a definition");
            }
            MekNHOres.LOGGER.debug("Loaded ore texture definition {} from {}", definition.material(), file);
            return true;
        } catch (IOException | RuntimeException e) {
            MekNHOres.LOGGER.error("Failed to load ore texture definition {} from {}", fileId, file, e);
            return false;
        }
    }

    @Nullable
    public static OreTextureDefinition get(String material) {
        return DEFINITIONS.get(material.toLowerCase(java.util.Locale.ROOT));
    }

    public static synchronized java.util.Collection<OreTextureDefinition> definitions() {
        return java.util.List.copyOf(DEFINITIONS.values());
    }
}
