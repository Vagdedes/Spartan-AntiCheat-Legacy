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

    void runMove(Entity target) {
        float yaw = RayUtils.castTo360(player.movement.getLocation().getYaw());
        float predYaw = RayUtils.set360Limit(getRotation(player.getInstance(), target)[0]);
        float diff = Math.abs(yaw - predYaw);
        float sinDiff = (float) Math.abs(Math.sin(yaw) * 360 - Math.sin(predYaw) * 360);
        // player.sendMessage("diff: " + diff + " diff2: " + sinDiff);
        // todo
    }

    private static float[] getRotation(Player player1, Entity entity2) {
        Vector direction = entity2.getLocation().toVector().subtract(player1.getLocation().toVector());

        float yaw = (float) Math.toDegrees(Math.atan2(direction.getX(), direction.getZ()));

        float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

        return new float[]{RayUtils.castTo360(yaw), pitch};
    }

}
