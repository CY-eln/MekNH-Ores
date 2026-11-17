package c_y.meknh_ores.api.item;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

/**
 * 把运行时生成的物品模型包成资源包。
 */
public final class ItemModelPackResources implements PackResources {

    private final PackLocationInfo location;
    private final Map<ResourceLocation, byte[]> resources;

    public ItemModelPackResources(PackLocationInfo location) {
        this.location = location;
        this.resources = ItemModelResources.build();
        this.resources.putAll(c_y.meknh_ores.api.model.OreTextureResources.build());
        this.resources.putAll(c_y.meknh_ores.api.block.OreBlockModelResources.build());
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return null;
    }

    @Nullable
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation resourceLocation) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }
        byte[] data = this.resources.get(resourceLocation);
        return data == null ? null : () -> new ByteArrayInputStream(data);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput resourceOutput) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        String prefix = path.isEmpty() ? "" : path + "/";
        this.resources.forEach((id, data) -> {
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                resourceOutput.accept(id, () -> new ByteArrayInputStream(data));
            }
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of(c_y.meknh_ores.MekNHOres.MODID) : Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public void close() {
    }
}
