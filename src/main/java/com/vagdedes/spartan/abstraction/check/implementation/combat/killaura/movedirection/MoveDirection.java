package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.RayLine;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Math.PI;

public class MoveDirection extends DetectionExecutor {

    private AttackHandle attackHandle;
    private LinkedList<AttackData> aimAnalysis = new LinkedList<>();
    private float vl;

    public MoveDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        attackHandle = new AttackHandle(null, System.currentTimeMillis());
    }

    public void runMove() {
        // player.sendMessage(String.valueOf(handle));
        if (attackHandle.getTimeMillis() + 3000 > System.currentTimeMillis()) {
            // main
            Entity target = attackHandle.getTarget();

            if (target != null) {
                float yaw = RayUtils.castTo360(player.movement.getLocation().getYaw());
                float predYaw = RayUtils.castTo360(calculate(player.movement.getLocation().toVector().toVector3d(),
                                target.getLocation().toVector().toVector3d()).x);
                double diff = Math.abs(yaw - predYaw);
                aimAnalysis.addFirst(new AttackData(diff, yaw, player.movement.getLocation().getPitch()));
                if (aimAnalysis.size() >= 10) analys(player.movement.getEventFromLocation());
                //player.sendMessage("diff: " + diff + " yaw: " + yaw + " predYaw: " + predYaw);
            }
        }
    }
    public void analys(SpartanLocation to) {
        float yawChange = Math.abs(aimAnalysis.getLast().getYaw() - aimAnalysis.getFirst().getYaw());
        float yawChangeBasic = 0;
        float oldYaw = aimAnalysis.getFirst().getYaw();
        float addVl = 0;
        double accuracy = 0;
        String punishment = null;
        for (AttackData data : aimAnalysis) {
            accuracy += data.getAccuracy();
            yawChangeBasic += Math.abs(data.getYaw() - oldYaw);
            oldYaw = data.getYaw();
        }
        accuracy /= 10;
        //player.sendMessage("yaw: " + yawChange + " accuracy: " + accuracy);
        aimAnalysis.clear();
        double factor = yawChange / accuracy;
        if (accuracy < 10 && yawChangeBasic > 50) {
           punishment = "heuristic(smart)";
           addVl = 205;
        }
        if (punishment != null) {
            vl += addVl;
            if (vl > 200) {
                cancel("type: move-direction, module: "
                                + punishment + ", accuracy: " + accuracy + ", factor: " + factor, 2, to);
            }
        } else {
            if (vl > 0) vl -= 30;
        }
        // player.sendMessage("accuracy: " + accuracy + ", factor: " + factor + ", yaw: " + yawChange);
    }

    public void runDamage(Entity targetHandle) {
        attackHandle = new AttackHandle(targetHandle, System.currentTimeMillis());
    }


    public Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.sub(from);
        final double distance = Math.hypot(diff.x(), diff.z());
        final float yaw = (float) (Math.atan2(diff.z(), diff.x()) * 180.0F / PI) - 90.0F;
        final float pitch = (float) (-(Math.atan2(diff.y(), distance) * 180.0F / PI));
        return new Vector2f(yaw, pitch);
    }

}