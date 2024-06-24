package com.vagdedes.spartan.listeners.bukkit;

import com.vagdedes.spartan.abstraction.world.SpartanBlockData;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Event_Chunks implements Listener {

    private static final Map<Long, Boolean> loaded = new ConcurrentHashMap<>();
    private static final Map<World, Map<Long, ChunkData>> map = new ConcurrentHashMap<>();
    private static final int chunkSize = (int) PlayerUtils.chunk;
    private static final SpartanBlockData empty = new SpartanBlockData(Material.AIR, false);

    // Separator

    private static final class ChunkData {

        private long creation;
        private final int x, z;
        private final SpartanBlockData[][][] blocks;

        private ChunkData(World world, ChunkSnapshot snapshot) {
            this.creation = System.currentTimeMillis();
            this.x = snapshot.getX();
            this.z = snapshot.getZ();
            this.blocks = new SpartanBlockData[chunkSize][1][chunkSize];
            set(world, snapshot);
        }

        private void refresh(World world) {
            if (System.currentTimeMillis() - this.creation > 1_000L) {
                this.creation = System.currentTimeMillis();
                this.set(world, world.getChunkAt(this.x, this.z).getChunkSnapshot());
            }
        }

        private void set(World world, ChunkSnapshot snapshot) {
            SpartanBukkit.chunksThread.execute(() -> {
                int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                        ? world.getMinHeight()
                        : 0,
                        maxHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                                ? world.getMaxHeight()
                                : PlayerUtils.height,
                        subtract;

                if (minHeight < 0) {
                    subtract = Math.abs(minHeight);
                    maxHeight += subtract;
                    minHeight = 0;
                } else {
                    subtract = 0;
                }
                for (int x = 0; x < chunkSize; x++) {
                    for (int z = 0; z < chunkSize; z++) {
                        for (int y = minHeight; y < maxHeight; y++) {
                            BlockData blockData = snapshot.getBlockData(x, y - subtract, z);
                            this.blocks[x][y][z] = new SpartanBlockData(
                                    blockData.getMaterial(),
                                    BlockUtils.isWaterLogged(blockData)
                            );
                        }
                    }
                }
            });
        }

    }

    // Separator

    static {
        if (SpartanBukkit.packetsForcedState) {
            SpartanBukkit.runRepeatingTask(() -> {
                if (SpartanBukkit.packetsEnabled()) {
                    for (Map.Entry<World, Map<Long, ChunkData>> entry : map.entrySet()) {
                        World world = entry.getKey();
                        for (ChunkData chunkData : entry.getValue().values()) {
                            chunkData.refresh(world);
                        }
                        for (Chunk chunk : world.getLoadedChunks()) {
                            if (!isLoaded(world, chunk.getX(), chunk.getZ())) {
                                cache(chunk);
                            }
                        }
                    }
                }
            }, 1L, 1L);
        }
    }

    // Separator

    private static long hashChunk(int x, int z) {
        return (31L * x) + z;
    }

    private static long totalHash(World world, int x, int z) {
        return (hashChunk(x, z) * 31L) + world.getName().hashCode();
    }

    // Separator

    public static void clear() {
        map.clear();
    }

    // Separator

    public static SpartanBlockData get(World world, int x, int y, int z) {
        int minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                ? world.getMinHeight()
                : 0;

        if (y < minHeight
                || y >= (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)
                ? world.getMaxHeight()
                : PlayerUtils.height)) {
            return empty;
        }
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            return empty;
        }
        ChunkData blockDataList = subMap.get(hashChunk(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

        if (blockDataList == null
                || y >= blockDataList.blocks[0].length) {
            return empty;
        }
        if (minHeight < 0) {
            y += Math.abs(minHeight);
        }
        return blockDataList.blocks[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize];
    }

    // Separator

    static void updateBlock(Block block, BlockData blockData) {
        World world = block.getWorld();
        int x = block.getX(),
                z = block.getZ();
        Map<Long, ChunkData> subMap = map.get(world);

        if (subMap == null) {
            cache(block.getChunk());
        } else {
            ChunkData chunkData = subMap.get(hashChunk(SpartanLocation.getChunkPos(x), SpartanLocation.getChunkPos(z)));

            if (chunkData == null) {
                cache(block.getChunk());
            } else {
                int y = block.getY(),
                        minHeight = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMinHeight() : 0;

                if (minHeight < 0) {
                    y += Math.abs(minHeight);
                }
                if (blockData == null) {
                    chunkData.blocks[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize] = new SpartanBlockData(
                            Material.AIR,
                            false
                    );
                } else {
                    chunkData.blocks[Math.abs(x) % chunkSize][y][Math.abs(z) % chunkSize] = new SpartanBlockData(
                            blockData.getMaterial(),
                            BlockUtils.isWaterLogged(blockData)
                    );
                }
            }
        }
    }

    // Separator

    public static boolean isLoaded(World world, int x, int z) {
        return loaded.containsKey(totalHash(world, x, z));
    }

    // Separator

    private static void cache(Chunk chunk) {
        World world = chunk.getWorld();
        map.computeIfAbsent(
                world,
                k -> new ConcurrentHashMap<>()
        ).put(
                hashChunk(chunk.getX(), chunk.getZ()),
                new ChunkData(world, chunk.getChunkSnapshot())
        );
    }

    // Separator

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Load(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.put(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()), null);

        if (SpartanBukkit.packetsEnabled()) {
            cache(chunk);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void Unload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        loaded.remove(totalHash(chunk.getWorld(), chunk.getX(), chunk.getZ()));

        if (SpartanBukkit.packetsEnabled()) {
            World world = chunk.getWorld();
            Map<Long, ChunkData> subMap = map.get(world);

            if (subMap != null
                    && subMap.remove(hashChunk(chunk.getX(), chunk.getZ())) != null
                    && subMap.isEmpty()) {
                map.remove(world);
            }
        }
    }

}
