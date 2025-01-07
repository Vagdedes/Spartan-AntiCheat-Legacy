package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.concurrent.SpartanScheduler;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

@Data
@RequiredArgsConstructor
public class EncirclementData {
    private boolean ice = false;
    private boolean slime = false;
    private boolean slimeWide = false;
    private boolean slimeHeight = false;
    private boolean water = false;
    private boolean semi = false;
    private boolean glide = false;
    private boolean jumpModify = false;
    private boolean climb = false;
    private boolean bubble = false;
    public EncirclementData(SpartanProtocol protocol) {
        Location location = protocol.getLocation();
        SpartanScheduler.run(() -> {
            double x = location.getX();
            double y = location.getY() - 0.1;
            double z = location.getZ();

            for (int dx = -2; dx <= 2; ++dx) {
                for (int dy = -2; dy <= 2; ++dy) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        boolean h = Math.abs(dx) < 2 && Math.abs(dz) < 2;
                        SpartanBlock b = new SpartanLocation(
                                        new Location(protocol.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                        ).getBlock();
                        Material material = protocol.packetWorld.getBlock(
                                        new Location(protocol.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                        );
                        Material materialFrom = protocol.packetWorld.getBlock(
                                        new Location(protocol.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                        );
                        Material m2 = b.getType();
                        if (material == null || materialFrom == null || m2 == null) return;

                        if ((BlockUtils.areHoneyBlocks(material)
                                        || BlockUtils.areBeds(material)
                                        || BlockUtils.areSlimeBlocks(material))) {
                            if (Math.abs(dy) < 2 && h) {
                                this.slime = true;
                            } else if (h) {
                                this.slimeHeight = true;
                            } else this.slimeWide = true;
                        }
                        if (Math.abs(dx) < 2
                                        && Math.abs(dz) < 2
                                        && (BlockUtils.areInteractiveBushes(material)
                                        || BlockUtils.areBeds(material)
                                        || BlockUtils.isPowderSnow(material))) {
                            this.jumpModify = true;
                        }
                        if (Math.abs(dy) < 2 && h
                                        && BlockUtils.canClimb(material, false)) {
                            this.climb = true;
                        }
                        if (Math.abs(dy) < 2 && h
                                        && (BlockUtils.isPowderSnow(material)
                                        || BlockUtils.areHoneyBlocks(material)
                                        || BlockUtils.areWebs(material)
                                        || BlockUtils.areInteractiveBushes(material))) {
                            this.glide = true;
                        }
                        if (Math.abs(dy) < 2 && h && !BlockUtils.canClimb(material, false) &&
                                        (BlockUtils.isSemiSolid(material)
                                                        || BlockUtils.areWalls(materialFrom)
                                                        || BlockUtils.areCobbleWalls(materialFrom)
                                                        || BlockUtils.areSlimeBlocks(material)
                                                        || BlockUtils.areHoneyBlocks(material)
                                                        || BlockUtils.areWebs(material))) {
                            this.semi = true;
                        }
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                                        && material.equals(Material.BUBBLE_COLUMN) || m2.equals(Material.BUBBLE_COLUMN)) {
                            if (h) this.bubble = true;
                        }
                        if (Math.abs(dy) < 2 && Math.abs(dx) < 2
                                        && Math.abs(dz) < 2
                                        && BlockUtils.isLiquidOrWaterLogged(b, true)
                                        || BlockUtils.isLiquid(material) || BlockUtils.isLiquid(m2)
                                        || BlockUtils.isWaterLogged(b)) {
                            this.water = true;
                        }
                        if (BlockUtils.areIceBlocks(material)) {
                            this.ice = true;
                        }
                    }
                }
            }
        });
    }
}
