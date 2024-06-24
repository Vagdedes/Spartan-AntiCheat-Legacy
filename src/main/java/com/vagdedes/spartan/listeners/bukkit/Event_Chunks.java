package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.world.SpartanBlockData;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Event_Chunks implements Listener {

    // todo extend with block place and block break
    private static final Map<Integer, Map<Long, SpartanBlockData[][][]>> map = new ConcurrentHashMap<>();
    private static final int chunkSize = (int) PlayerUtils.chunk;
    private static final SpartanBlockData empty = new SpartanBlockData(Material.AIR, false);

    private static long hashChunk(int x, int z) {
        return (31L * x) + z;
    }

    private static int hashWorld(World world) {
        return world.getName().hashCode();
    }

    public static void clear() {
        map.clear();
    }

    public static SpartanBlockData get(World world, int x, int y, int z) {
        int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMinHeight() : 0;

        if (y < minHeight
                || y >= (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMaxHeight() : PlayerUtils.height)) {
            return empty;
        }
        x = SpartanLocation.getChunkPos(x);
        z = SpartanLocation.getChunkPos(z);
        Map<Long, SpartanBlockData[][][]> subMap = map.get(hashWorld(world));

        if (subMap == null) {
            return empty;
        }
        SpartanBlockData[][][] blockDataList = subMap.get(hashChunk(x, z));

        if (blockDataList == null || y >= blockDataList[0].length) {
            return empty;
        }

        if (minHeight < 0) {
            y += Math.abs(minHeight);
        }
        return blockDataList[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize];
    }

    public static void check(World world, int x, int z) {
        Map<Long, SpartanBlockData[][][]> subMap = map.get(hashWorld(world));

        if (subMap == null
                || !subMap.containsKey(hashChunk(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)))) {
            cache(world.getChunkAt(x, z));
        }
    }

    public static void set(World world, int x, int y, int z, BlockData blockData) { // todo
        int chunkX = SpartanLocation.getChunkPos(x),
                chunkZ = SpartanLocation.getChunkPos(z);
        Map<Long, SpartanBlockData[][][]> subMap = map.get(hashWorld(world));

        if (subMap == null) {
            cache(world.getChunkAt(x, z));
        } else {
            SpartanBlockData[][][] blockDataList = subMap.get(hashChunk(x, z));

            if (blockDataList == null) {
                cache(world.getChunkAt(x, z));
            } else {
                int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMinHeight() : 0,
                        maxHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMaxHeight() : PlayerUtils.height;

                if (minHeight < 0) {
                    y += Math.abs(minHeight);
                }
                if (y >= maxHeight) {
                    return;
                }
                blockDataList[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize] = new SpartanBlockData(
                        blockData.getMaterial(),
                        SpartanLocation.isWaterLogged(blockData)
                );
            }
        }
    }

    private static void cache(Chunk chunk) {
        int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? chunk.getWorld().getMinHeight() : 0,
                maxHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? chunk.getWorld().getMaxHeight() : PlayerUtils.height;
        Map<Long, SpartanBlockData[][][]> subMap = map.computeIfAbsent(
                hashWorld(chunk.getWorld()),
                k -> new ConcurrentHashMap<>()
        );
        int subtract;

        if (minHeight < 0) {
            subtract = Math.abs(minHeight);
            maxHeight += subtract;
            minHeight = 0;
        } else {
            subtract = 0;
        }
        SpartanBlockData[][][] blockDataList = new SpartanBlockData[chunkSize][maxHeight][chunkSize];

        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    BlockData blockData = chunk.getBlock(x, y - subtract, z).getBlockData();
                    blockDataList[x][y][z] = new SpartanBlockData(
                            blockData.getMaterial(),
                            SpartanLocation.isWaterLogged(blockData)
                    );
                }
            }
        }
        subMap.put(
                hashChunk(chunk.getX(), chunk.getZ()),
                blockDataList
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Load(ChunkLoadEvent e) {
        if (SpartanBukkit.packetsEnabled()) {
            Chunk chunk = e.getChunk();

            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Player) {
                    cache(chunk);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Unload(ChunkUnloadEvent e) {
        if (SpartanBukkit.packetsEnabled()) {
            Chunk chunk = e.getChunk();
            int worldHash = hashWorld(chunk.getWorld());
            Map<Long, SpartanBlockData[][][]> subMap = map.get(worldHash);

            if (subMap != null
                    && subMap.remove(hashChunk(chunk.getX(), chunk.getZ())) != null
                    && subMap.isEmpty()) {
                map.remove(worldHash);
            }
        }
    }

}
