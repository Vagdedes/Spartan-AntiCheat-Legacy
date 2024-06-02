package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.RayUtils;
import org.bukkit.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.LinkedList;

import static java.lang.Math.PI;

public class MoveDirection extends DetectionExecutor {

    private AttackHandle attackHandle;
    private final LinkedList<AttackData> aimAnalysis;
    private float vl;
    private final LinkedList<Double> rayTraceAnalysis = new LinkedList<>();

    public MoveDirection(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        aimAnalysis = new LinkedList<>();
        attackHandle = new AttackHandle(null, System.currentTimeMillis());
    }

    public void runMove() {
        // player.sendMessage(String.valueOf(handle));
        if (attackHandle.getTimeMillis() + 3000 > System.currentTimeMillis()) {
            // main
            Entity target = attackHandle.getTarget();

            if (target != null) {
                double bruteforceRayTrace = RayUtils.bruteforceRayTrace(player, target);
                double diff = bruteforceRayTrace - 0.4;
                rayTraceAnalysis.addLast(bruteforceRayTrace);
                aimAnalysis.addLast(new AttackData(diff,
                                player.movement.getLocation().getYaw(),
                                player.movement.getLocation().getPitch()));
                if (rayTraceAnalysis.size() >= 10) {
                    analyze(player.movement.getLocation(), bruteforceRayTrace);
                }
            }
        }
    }
    public void analyze(SpartanLocation to, double bruteForce) {
        String punish = null;
        double globalValue = 0;
        double changeYaw = 0;
        double oldYaw = aimAnalysis.getFirst().getYaw();
        double oldYawResult = aimAnalysis.getFirst().getYaw();
        int basicHeuristic = 0;
        for (Double value : rayTraceAnalysis) {
            globalValue += value;
        }
        for (AttackData value : aimAnalysis) {
            changeYaw += Math.abs(value.getYaw() - oldYaw);
            oldYaw = value.getYaw();
        }
        for (int i = 0; i < aimAnalysis.size(); i++) {
            double yawChange = Math.abs(aimAnalysis.get(i).getYaw() - oldYawResult);

            if (rayTraceAnalysis.get(i) < 0.1 && changeYaw > 7) {
                basicHeuristic++;
            }

            oldYawResult = aimAnalysis.get(i).getYaw();
        }
        double result = globalValue / rayTraceAnalysis.size();
        if (basicHeuristic > 6) punish = "heuristic(basic)";
        if (punish != null) {
            cancel(
                            "type: " + punish + ", hitbox-size-analysis: "
                                            + result + ", hitbox-size-last: " + bruteForce,
                            1.0,
                            to
            );
        }
        rayTraceAnalysis.clear();
        aimAnalysis.clear();

    }

    public void runDamage(Entity targetHandle) {
        attackHandle = new AttackHandle(targetHandle, System.currentTimeMillis());
    }

    public Vector2f calculate(final Vector3d from, final Vector3d to) {
        Vector3d diff = to.sub(from);
        double distance = Math.hypot(diff.x(), diff.z());
        float yaw = (float) (Math.atan2(diff.z(), diff.x()) * 180.0F / PI) - 90.0F,
                pitch = (float) (-(Math.atan2(diff.y(), distance) * 180.0F / PI));
        return new Vector2f(yaw, pitch);
    }

}