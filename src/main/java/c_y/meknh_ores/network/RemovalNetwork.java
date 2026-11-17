package c_y.meknh_ores.network;

import c_y.meknh_ores.data.RemovalData;
import c_y.meknh_ores.resource.UnifiedResourceIndex;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class RemovalNetwork {
    private RemovalNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(
                RemovalSyncPayload.TYPE,
                RemovalSyncPayload.STREAM_CODEC,
                RemovalNetwork::handle
        );
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, RemovalData.snapshot());
        }
    }

    /** Used when KubeJS changes the list after players are already online. */
    public static void broadcastIfServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            PacketDistributor.sendToAllPlayers(RemovalData.snapshot());
        }
    }

    private static void handle(RemovalSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            RemovalData.applySnapshot(payload);
            UnifiedResourceIndex.invalidate();
            if (FMLEnvironment.dist.isClient()) {
                try {
                    Class.forName("c_y.meknh_ores.client.ClientRemovalSync")
                            .getMethod("reapplyJei")
                            .invoke(null);
                } catch (Throwable ignored) {
                    // JEI is optional; nothing to refresh when it is absent.
                }
            }
        });
    }
}
