package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MoveDirection extends DetectionExecutor {

    public MoveDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
    }

    Map<Player, AttackHandle> container = new ConcurrentHashMap<>();

    public void runMove(Entity targetHandle, Boolean handle) {
        //player.sendMessage(String.valueOf(handle));
        if (handle) {
            container.put(player.getInstance(), new AttackHandle(targetHandle, System.currentTimeMillis()));
        } else if (container.containsKey(player.getInstance())
                        && container.get(player.getInstance()).getTimeMillis() + 3000 > System.currentTimeMillis()) {
            // main
            Entity target = container.get(player.getInstance()).getTarget();
            float diff = Math.abs(RayUtils.castTo360(player.movement.getLocation().getYaw()
                            - getRotation(player.getInstance(), target)[0]));
            float yaw = RayUtils.castTo360(player.movement.getLocation().getYaw());
            float predYaw = getRotation(player.getInstance(), target)[0];
            // player.sendMessage("diff: " + diff + " " + Math.sin(yaw) + " " + Math.sin(predYaw));
        }
    }

    private static float[] getRotation(Player player1, Entity entity2) {
        Vector direction = entity2.getLocation().toVector().subtract(player1.getLocation().toVector());

        float yaw = (float) Math.toDegrees(Math.atan2(direction.getX(), direction.getZ()));

        float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));

        return new float[]{RayUtils.castTo360(yaw), pitch};
    }

}
