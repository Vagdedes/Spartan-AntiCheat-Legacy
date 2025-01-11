package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.data.CheckBoundData;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.AxisAlignedBB;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Set;

public class BlockRaytrace extends ProbabilityDetection {

    BlockRaytrace(CheckRunner executor) {
        super(executor, null, null, "block_raytrace", true);
    }

    public void run(Entity targetHandle) {
        if (true) {
            return;
        }
        if (targetHandle instanceof Player) {
            SpartanProtocol target = SpartanBukkit.getProtocol((Player) targetHandle);

            if (target != null) {
                Location playerLocation = this.protocol.getLocation();
                worldInit(this.protocol, playerLocation);
                this.protocol.setCheckBoundData(new CheckBoundData(this.protocol, target));
                Vector result = isBoundThrough(this.protocol.getCheckBoundData());
                if (result != null) {
                    cancel("attack through block at " + result);
                    this.protocol.setCheckBoundData(null);
                }
            }
        }
    }

    public static void worldInit(SpartanProtocol iPlayer, Location location) {
        World world = location.getWorld();
        for (int x = location.getBlockX() - 3; x <= location.getBlockX() + 3; x++) {
            for (int y = location.getBlockY() - 3; y <= location.getBlockY() + 3; y++) {
                for (int z = location.getBlockZ() - 3; z <= location.getBlockZ() + 3; z++) {
                    Material material = new SpartanLocation(world, x, y, z).getBlock().getTypeOrNull();
                    double s = 0.5;

                    if (material.isSolid() && !ignore(material)) {
                        iPlayer.getAxisMatrixCache().add(
                                new AxisAlignedBB(
                                        x - s, y - s, z - s,
                                        x + s, y + s, z + s)
                        );
                    }
                }
            }
        }
    }

    public static Vector isBoundThrough(CheckBoundData data) {
        if (data == null && data.getAttacker().getAxisMatrixCache() == null) return null;
        SpartanProtocol attacker = data.getAttacker();
        Set<AxisAlignedBB> blocks = attacker.getAxisMatrixCache();
        Location l = data.getTarget();
        AxisAlignedBB target;
        double size = 0.75;
        target = new AxisAlignedBB(
                l.getX() - size, l.getY() - 0.3, l.getZ() - size,
                l.getX() + size, l.getY() + 2.3, l.getZ() + size);
        for (double d = 0.0; d < 3.0; d += 0.01) {
            if (RayUtils.ifRayBoundClient(attacker, target, d)) {
                return null;
            }
            for (AxisAlignedBB bb : blocks) {
                if (RayUtils.ifRayBound(attacker, bb, d)
                        && !bb.isEqual(target)) {
                    return bb.toVector();
                }
            }
        }
        return null;
    }

    private static boolean ignore(Material m) {
        return BlockUtils.isSemiSolid(m);
        //return block.matches(".*(snow|step|frame|table|slab|stair|ladder|vine|waterlily|wall|carpet|fence|rod|bed|skull|pot|hopper|door|piston|lily|slime).*");
    }

}
