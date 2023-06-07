package me.vagdedes.spartan.objects.replicates;

import me.vagdedes.spartan.features.important.MultiVersion;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.server.MaterialUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;

public class SpartanBlock {

    private final SpartanPlayer player;
    private final Material material;
    private final World world;
    private Chunk chunk;
    private final byte data;
    private final int x;
    private final short y;
    private final int z;
    private final boolean liquid, waterLogged;

    SpartanBlock(SpartanPlayer player, World world, Chunk chunk, Material material, byte data, int x, int y, int z, boolean liquid, boolean waterLogged) {
        this.player = player;
        this.world = world;
        this.material = material;
        this.chunk = chunk;
        this.data = data;
        this.x = x;
        this.y = (short) y;
        this.z = z;
        this.liquid = liquid;
        this.waterLogged = waterLogged;
    }

    public SpartanBlock(SpartanPlayer player, Block block) {
        this.player = player;
        this.world = block.getWorld();
        this.material = block.getType();
        this.chunk = null;
        this.x = block.getX();
        this.y = (short) block.getY();
        this.z = block.getZ();
        this.liquid = block.isLiquid();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            Object blockData = block.getBlockData();
            this.waterLogged = blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
            this.data = (byte) (blockData instanceof Levelled ? ((Levelled) blockData).getLevel() : 0);
        } else {
            this.waterLogged = false;
            this.data = block.getData();
        }
    }

    public Chunk getChunk() {
        if (chunk == null) {
            chunk = getLocation().getChunk();
        }
        return chunk;
    }

    public World getWorld() {
        return world;
    }

    public Material getType() {
        return material;
    }

    public byte getData() {
        return data;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public boolean isWaterLogged() {
        return waterLogged;
    }

    public boolean isLiquid() {
        return liquid || waterLogged || (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? this.material == Material.WATER || this.material == Material.LAVA :
                this.material == Material.getMaterial("STATIONARY_WATER") || this.material == Material.getMaterial("STATIONARY_LAVA"));
    }

    public SpartanLocation getLocation() {
        return new SpartanLocation(this.player, this.world, this.chunk, this.x, this.y, this.z, 0.0f, 0.0f, this);
    }

    public Block getBlock() {
        SpartanLocation loc = getLocation();
        return loc.getWorld().getBlockAt(loc.getLimitedBukkitLocation());
    }

    // Custom

    // Identifiers

    public boolean isSurfaceBlock() {
        Material m = getType();
        return m == MaterialUtils.get("grass_block")
                || m == MaterialUtils.get("soil")
                || m == Material.DIRT
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && (m == Material.COARSE_DIRT || m == Material.ANDESITE || m == Material.POLISHED_ANDESITE)
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9) && m == Material.getMaterial("GRASS_PATH")
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) && m == Material.DIRT_PATH
                || BlockUtils.areEntityBlocks(m)
                || BlockUtils.areTerracotta(m)
                || BlockUtils.areConcrete(m);
    }
}
