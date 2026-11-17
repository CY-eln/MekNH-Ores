package c_y.meknh_ores.network;

import c_y.meknh_ores.MekNHOres;
import c_y.meknh_ores.resource.ResourceKind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sends the server-side cleanup lists to the client.  This is required for JEI when KubeJS or a
 * data pack adds entries that only exist on the server (not in the client's bundled files).
 */
public record RemovalSyncPayload(Map<ResourceKind, Set<String>> materials,
                                 Set<ResourceLocation> items,
                                 Set<ResourceLocation> blocks,
                                 Set<ResourceLocation> itemTags,
                                 Set<ResourceLocation> blockTags,
                                 Set<String> namespaces,
                                 Set<String> exemptNamespaces)
        implements CustomPacketPayload {
    public static final Type<RemovalSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MekNHOres.MODID, "removal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemovalSyncPayload> STREAM_CODEC =
            StreamCodec.of(RemovalSyncPayload::write, RemovalSyncPayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, RemovalSyncPayload payload) {
        buffer.writeVarInt(payload.materials.size());
        for (Map.Entry<ResourceKind, Set<String>> entry : payload.materials.entrySet()) {
            buffer.writeEnum(entry.getKey());
            buffer.writeVarInt(entry.getValue().size());
            for (String material : entry.getValue()) {
                buffer.writeUtf(material);
            }
        }

        writeIds(buffer, payload.items);
        writeIds(buffer, payload.blocks);
        writeIds(buffer, payload.itemTags);
        writeIds(buffer, payload.blockTags);

        buffer.writeVarInt(payload.namespaces.size());
        for (String namespace : payload.namespaces) {
            buffer.writeUtf(namespace);
        }

        buffer.writeVarInt(payload.exemptNamespaces.size());
        for (String namespace : payload.exemptNamespaces) {
            buffer.writeUtf(namespace);
        }
    }

    private static void writeIds(RegistryFriendlyByteBuf buffer, Set<ResourceLocation> ids) {
        buffer.writeVarInt(ids.size());
        for (ResourceLocation id : ids) {
            buffer.writeResourceLocation(id);
        }
    }

    private static RemovalSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int materialKinds = buffer.readVarInt();
        Map<ResourceKind, Set<String>> materials = new EnumMap<>(ResourceKind.class);
        for (int i = 0; i < materialKinds; i++) {
            ResourceKind kind = buffer.readEnum(ResourceKind.class);
            int count = buffer.readVarInt();
            Set<String> values = new LinkedHashSet<>();
            for (int j = 0; j < count; j++) {
                values.add(buffer.readUtf());
            }
            materials.put(kind, values);
        }

        Set<ResourceLocation> items = readIds(buffer);
        Set<ResourceLocation> blocks = readIds(buffer);
        Set<ResourceLocation> itemTags = readIds(buffer);
        Set<ResourceLocation> blockTags = readIds(buffer);

        int namespaceCount = buffer.readVarInt();
        Set<String> namespaces = new LinkedHashSet<>();
        for (int i = 0; i < namespaceCount; i++) {
            namespaces.add(buffer.readUtf());
        }

        int exemptCount = buffer.readVarInt();
        Set<String> exempt = new LinkedHashSet<>();
        for (int i = 0; i < exemptCount; i++) {
            exempt.add(buffer.readUtf());
        }

        return new RemovalSyncPayload(materials, items, blocks, itemTags, blockTags,
                namespaces, exempt);
    }

    private static Set<ResourceLocation> readIds(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            ids.add(buffer.readResourceLocation());
        }
        return ids;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
