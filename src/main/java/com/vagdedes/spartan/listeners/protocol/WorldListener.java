package com.vagdedes.spartan.listeners.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedLevelChunkData;
import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    public static void commitNewWorldListener(Player player) {
        chunkContainer.put(player.getUniqueId(), new HashMap<>());
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PacketContainer container = event.getPacket();
        if (container.getType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            int chunkX = container.getChunkCoordIntPairs().read(0).getChunkX();
            int chunkZ = container.getChunkCoordIntPairs().read(0).getChunkZ();
            chunkContainer.get(uuid).remove(mutate(chunkX, chunkZ));
        } else {
            int chunkX = container.getIntegers().read(0);
            int chunkZ = container.getIntegers().read(1);
            StructureModifier<WrappedLevelChunkData.ChunkData> chunk = container.getLevelChunkData();
            chunkContainer.computeIfAbsent(uuid, k -> new HashMap<>()).put(mutate(chunkX, chunkZ), chunk);
        }
    }

    public static StructureModifier<WrappedLevelChunkData.ChunkData> getChunkStructure(Player player) {
        SpartanPlayer spartanPlayer = SpartanBukkit.getPlayer(player);
        int x = spartanPlayer.protocol.getLocation().getChunk().getX();
        int z = spartanPlayer.protocol.getLocation().getChunk().getZ();

        if (!chunkContainer.containsKey(player.getUniqueId())) return null;

        Map<Integer, StructureModifier<WrappedLevelChunkData.ChunkData>> playerChunks = chunkContainer.get(player.getUniqueId());

        if (!playerChunks.containsKey(mutate(x, z))) return null;

        return playerChunks.get(mutate(x, z));
    }
    private static int mutate(int x, int y) {
        return (Objects.hashCode(x) + Objects.hashCode(y)) / 2;
    }

}
