package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
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
            double distToTarget = playerLocation.distance(targetLocation);
            Material result = intercept(player, playerLocation, targetLocation, distToTarget);
            if (result != null) {
                cancel("Attacking through block: " + result);
            }
        }
    }
    public static Material intercept(SpartanProtocol iPlayer, Location location, Location targetLoc, double distToTarget) {
        for (int x = location.getBlockX() - 2; x <= location.getBlockX() + 2; x++) {
            for (int y = location.getBlockY() - 2; y <= location.getBlockY() + 2; y++) {
                for (int z = location.getBlockZ() - 2; z <= location.getBlockZ() + 2; z++) {
                    if (location.getWorld().getBlockAt(x, y, z).getType().isSolid()
                                    && !ignore(location.getWorld().getBlockAt(x, y, z).getType())) {
                        double distToBlock = location.distance(new Location(location.getWorld(), x, y, z));
                        if (distToTarget > 0.5 && RayUtils.inHitbox(iPlayer.spartanPlayer, targetLoc, 0.5F))
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
