package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class BlockRaytrace extends DetectionExecutor {

    BlockRaytrace(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    public void runDamage(Entity targetHandle) {
        if (targetHandle instanceof Player) {
            SpartanProtocol player = SpartanBukkit.getProtocol(this.player.getInstance());
            SpartanProtocol target = SpartanBukkit.getProtocol((Player) targetHandle);
            Location playerLocation = player.getLocation();
            Location targetLocation = target.getLocation();
            boolean valid = true;
            Material block = null;
            if (playerLocation.distance(targetLocation) < 2) return;
            for (double d = 0.3D; d <= 1.5D; d += 0.1D) {
                Material result = intercept(player, playerLocation, targetLocation, d);
                if (result != null) {
                    valid = false;
                    block = result;
                    break;
                } else if (RayUtils.inHitbox(player.spartanPlayer, targetLocation, 0.6F, d)) {
                    break;
                }
            }
            if (!valid) {
                cancel("type: block-raytrace, attacking-through-block: " + block);
            }
        }
    }
    public static Material intercept(SpartanProtocol iPlayer, Location location, Location targetLoc, double distToTarget) {
        for (int x = location.getBlockX() - 2; x <= location.getBlockX() + 2; x++) {
            for (int y = location.getBlockY() - 2; y <= location.getBlockY() + 2; y++) {
                for (int z = location.getBlockZ() - 2; z <= location.getBlockZ() + 2; z++) {
                    Block b = location.getWorld().getBlockAt(x, y, z);
                    if (b.getType().isSolid() && !ignore(b.getType())) {
                        if (RayUtils.inHitbox(iPlayer.spartanPlayer, b.getLocation(), 0.5F, distToTarget))
                            return location.getWorld().getBlockAt(x, y, z).getType();
                    }
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
