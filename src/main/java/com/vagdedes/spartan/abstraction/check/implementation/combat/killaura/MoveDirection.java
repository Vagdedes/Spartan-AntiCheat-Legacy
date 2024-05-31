package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MoveDirection extends DetectionExecutor {

    MoveDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    void runMove() {
        for (Entity target : player.getNearbyEntities(1, 1, 1)) {
            float diff = Math.abs(RayUtils.castTo360(player.movement.getLocation().getYaw()
                    - getRotation(player.getInstance(), target)[0]));
            float yaw = RayUtils.castTo360(player.movement.getLocation().getYaw());
            float predYaw = getRotation(player.getInstance(), target)[0];
            //player.sendMessage("diff: " + diff + " " + Math.sin(yaw) + " " + Math.sin(predYaw));
            // todo
        }
    }

    private static float[] getRotation(Player player1, Entity entity2) {
        Vector direction = entity2.getLocation().toVector().subtract(player1.getLocation().toVector());

        float yaw = (float) Math.toDegrees(Math.atan2(direction.getX(), direction.getZ()));

        float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

        return new float[]{RayUtils.castTo360(yaw), pitch};
    }

}
