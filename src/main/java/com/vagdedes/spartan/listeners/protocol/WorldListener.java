package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldListener extends PacketAdapter {

    private static final Map<UUID, Map<Integer,
            StructureModifier<WrappedLevelChunkData.ChunkData>>>
            chunkContainer = new ConcurrentHashMap<>();

    public WorldListener() {
        super(
                Register.plugin,
                ListenerPriority.HIGHEST,
                PacketType.Play.Server.MAP_CHUNK,
                PacketType.Play.Server.CHUNK_BATCH_START, PacketType.Play.Server.UNLOAD_CHUNK
        );
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer container = event.getPacket();

        if (container.getType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            int chunkX = container.getChunkCoordIntPairs().read(0).getChunkX(),
                    chunkZ = container.getChunkCoordIntPairs().read(0).getChunkZ();
            Map<Integer, StructureModifier<WrappedLevelChunkData.ChunkData>> playerChunks =
                    chunkContainer.get(event.getPlayer().getUniqueId());

            if (playerChunks != null) {
                playerChunks.remove(hash(chunkX, chunkZ));
            }
        } else {
            int chunkX = container.getIntegers().read(0),
                    chunkZ = container.getIntegers().read(1);
            StructureModifier<WrappedLevelChunkData.ChunkData> chunk = container.getLevelChunkData();
            chunkContainer.computeIfAbsent(
                    event.getPlayer().getUniqueId(),
                    k -> new LinkedHashMap<>()
            ).put(hash(chunkX, chunkZ), chunk);
        }
    }

    public static StructureModifier<WrappedLevelChunkData.ChunkData> getChunkStructure(Player player) {
        int x = SpartanLocation.getChunkPos(player.getLocation().getBlockX()),
                z = SpartanLocation.getChunkPos(player.getLocation().getBlockZ());
        Map<Integer, StructureModifier<WrappedLevelChunkData.ChunkData>> playerChunks =
                chunkContainer.get(player.getUniqueId());
        return playerChunks == null
                ? null
                : playerChunks.get(hash(x, z));
    }

    private static int hash(int x, int z) {
        return (x * SpartanBukkit.hashCodeMultiplier) + z;
    }

}
