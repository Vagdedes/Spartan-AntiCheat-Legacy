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
                double predict = RayUtils.castTo360(calculate(player.movement.getLocation().toVector().toVector3d(),
                                target.getLocation().toVector().toVector3d()).x);
                double yaw360 = RayUtils.castTo360(player.movement.getLocation().getYaw());
                rayTraceAnalysis.addLast(bruteforceRayTrace);
                aimAnalysis.addLast(new AttackData(Math.abs(predict - yaw360),
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
        double yawChangeFirst = Math.abs(aimAnalysis.get(0).getYaw() - aimAnalysis.get(1).getYaw());
        int basicHeuristic = 0;
        int lowHeuristic = 0;
        int hugeMoveAmount = 0;
        int gcd = 0;
        int aggressivePatternI = 0;
        int aggressivePatternD = 0;
        int aggressivePatternI2 = 0;
        int aggressivePatternD2 = 0;
        int robotizedAmount = 0;
        for (Double value : rayTraceAnalysis) {
            globalValue += value;
        }
        for (AttackData value : aimAnalysis) {
            changeYaw += Math.abs(value.getYaw() - oldYaw);
            oldYaw = value.getYaw();
        }
        double oldYawChange = Math.abs(aimAnalysis.get(0).getYaw() - oldYawResult);
        for (int i = 0; i < aimAnalysis.size(); i++) {
            double yawChange = Math.abs(aimAnalysis.get(i).getYaw() - oldYawResult);
            double robotized = Math.abs(yawChange - yawChangeFirst);
            double diffBetweenYawChanges = yawChange - oldYawChange;
            // player.sendMessage("ray: " + rayTraceAnalysis.get(i) + ", yaw: " + yawChange);
            // player.sendMessage("d: " + RayUtils.scaleVal(diffBetweenYawChanges, 2));
            if (robotized < 2 && yawChange > 2.5 && aimAnalysis.get(i).getAccuracy() < 10) robotizedAmount += 1;
            if (rayTraceAnalysis.get(i) < 0.1 && yawChange > 5) {
                basicHeuristic++;
            }
            if (rayTraceAnalysis.get(i) < 0.03 && yawChange > 1) {
                lowHeuristic++;
            }
            if (yawChange == 0.1) gcd++;
            if (diffBetweenYawChanges > 0.1 && diffBetweenYawChanges < 2) aggressivePatternI++;
            if (diffBetweenYawChanges < -0.1 && diffBetweenYawChanges > -2) aggressivePatternD++;
            if (diffBetweenYawChanges > 2) aggressivePatternI2++;
            if (diffBetweenYawChanges < -2) aggressivePatternD2++;

            oldYawResult = aimAnalysis.get(i).getYaw();
            oldYawChange = yawChange;
        }
        // player.sendMessage("huge: " + hugeMoveAmount + ", low: " + lowMoveAmount);
        double result = globalValue / rayTraceAnalysis.size();
        if (basicHeuristic > 6) punish = "heuristic(basic)";
        if (lowHeuristic > 7) punish = "heuristic(low)";
        if (robotizedAmount > 7) punish = "heuristic(sync)";
        if (gcd > 2) punish = "pattern(gcd)";
        if (aggressivePatternI > 3 && aggressivePatternD > 3) punish = "pattern(random)";
        if (aggressivePatternI2 > 3 && aggressivePatternD2 > 3
                        && (aggressivePatternI2 + aggressivePatternD2) > 8) punish = "pattern(snap)";
        // player.sendMessage(aggressivePatternI + " " + aggressivePatternD);
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