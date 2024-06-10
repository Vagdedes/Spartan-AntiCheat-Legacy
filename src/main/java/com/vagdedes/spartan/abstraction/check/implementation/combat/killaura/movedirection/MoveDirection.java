package com.vagdedes.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2F;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector3D;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.PI;

public class MoveDirection extends DetectionExecutor {

    private AttackHandle attackHandle;
    private final LinkedList<AttackData> aimAnalysis;
    private float vl = 0;
    private String punish = "";
    private double lastSwitchYaw = 0;
    private double lastSwitchPitch = 0;
    private double switchYaw = 0;
    private double switchPitch = 0;
    private List<UUID> targetsOnTheRow = new ArrayList<>();
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
                double predict = RayUtils.castTo360(
                        calculate(
                                player.movement.getLocation().toVector3D(),
                                new SpartanLocation(target.getLocation()).toVector3D()
                        ).x
                );
                double yaw360 = RayUtils.castTo360(player.movement.getLocation().getYaw());
                rayTraceAnalysis.addLast(bruteforceRayTrace);
                aimAnalysis.addLast(new AttackData(Math.abs(predict - yaw360),
                        player.movement.getLocation().getYaw(),
                        player.movement.getLocation().getPitch(), System.currentTimeMillis(), target.getUniqueId()));
                if (rayTraceAnalysis.size() >= 10) {
                    analyze(player.movement.getLocation(), bruteforceRayTrace);
                }
            }
        }
    }

    public void analyze(SpartanLocation to, double bruteForce) {
        targetsOnTheRow.clear();
        punish = "";
        double globalValue = 0;
        double changeYaw = 0;
        double oldYaw = aimAnalysis.getFirst().getYaw();
        double oldYawResult = aimAnalysis.getFirst().getYaw();
        double oldPitchResult = aimAnalysis.getFirst().getPitch();
        double yawChangeFirst = Math.abs(aimAnalysis.get(0).getYaw() - aimAnalysis.get(1).getYaw());
        int basicHeuristic = 0;
        int lowHeuristic = 0;
        int machineKnownMovement = 0;
        int constantRotations = 0;
        int gcd = 0;
        int aggressivePatternI = 0;
        int aggressivePatternD = 0;
        int aggressivePatternI2 = 0;
        int aggressivePatternD2 = 0;
        int robotizedAmount = 0;
        int aggressiveAim = 0;
        for (Double value : rayTraceAnalysis) {
            globalValue += value;
        }
        for (AttackData value : aimAnalysis) {
            changeYaw += Math.abs(value.getYaw() - oldYaw);
            oldYaw = value.getYaw();
        }
        double oldYawChange = Math.abs(aimAnalysis.get(0).getYaw() - oldYawResult);
        double oldPitchChange = Math.abs(aimAnalysis.get(0).getPitch() - oldPitchResult);
        for (int i = 0; i < aimAnalysis.size(); i++) {
            if (!targetsOnTheRow.contains(aimAnalysis.get(i).getTarget()))
                targetsOnTheRow.add(aimAnalysis.get(i).getTarget());
            double yawChange = Math.abs(aimAnalysis.get(i).getYaw() - oldYawResult);
            double pitchChange = Math.abs(aimAnalysis.get(i).getPitch() - oldPitchResult);
            double expectedYaw = yawChange * 1.073742f + (float) (yawChange + 0.15);
            double expectedPitch = pitchChange * 1.073742f - (float) (pitchChange - 0.15);
            double yawDiff = Math.abs(yawChange - expectedYaw);
            double pitchDiff = Math.abs(pitchChange - expectedPitch);
            switchYaw = Math.abs(yawChange - yawDiff);
            switchPitch = Math.abs(pitchChange - pitchDiff);
            double switchDiffOnYaw = Math.abs(switchYaw - lastSwitchYaw);
            if (switchDiffOnYaw > 26 && (aimAnalysis.get(i).getTime()
                    + 75 > attackHandle.getTimeMillis())) addNewPunish("heuristic(switch)", 35);

            double robotized = Math.abs(yawChange - yawChangeFirst);
            double diffBetweenYawChanges = yawChange - oldYawChange;
            double diffBetweenPitchChanges = pitchChange - oldPitchChange;
            if (robotized < 2 && yawChange > 2.5
                    && aimAnalysis.get(i).getAccuracy() < 10) robotizedAmount += 1;

            if (rayTraceAnalysis.get(i) < 0.1 && yawChange > 5) basicHeuristic++;

            if (rayTraceAnalysis.get(i) < 0.03 && yawChange > 1) lowHeuristic++;

            if (robotized < 0.99 && yawChange > 3) machineKnownMovement++;
            if (robotized < 0.02 && yawChange > 2) constantRotations++;
            if (robotized < 2 && yawChange > 3) aggressiveAim++;

            if (RayUtils.scaleVal(yawChange, 2) == 0.1F || RayUtils.scaleVal(pitchChange, 2) == 0.1F) gcd++;
            if ((diffBetweenYawChanges > 0.01 && diffBetweenYawChanges < 2)) aggressivePatternI++;
            if ((diffBetweenYawChanges < -0.01 && diffBetweenYawChanges > -2)) aggressivePatternD++;
            if (diffBetweenYawChanges > 2) aggressivePatternI2++;
            if (diffBetweenYawChanges < -2) aggressivePatternD2++;
            oldYawResult = aimAnalysis.get(i).getYaw();
            oldPitchResult = aimAnalysis.get(i).getPitch();
            oldYawChange = yawChange;
            oldPitchChange = pitchChange;
            lastSwitchYaw = switchYaw;
            lastSwitchPitch = switchPitch;
        }
        // player.sendMessage("huge: " + hugeMoveAmount + ", low: " + lowMoveAmount);
        double result = globalValue / rayTraceAnalysis.size();
        if (targetsOnTheRow.size() < 3)  {
            if (basicHeuristic > 6) addNewPunish("heuristic(basic)", 210);
            if (lowHeuristic > 7) addNewPunish("heuristic(low)", 210);
            if (robotizedAmount > 7) addNewPunish("heuristic(sync)", 170);
            if (machineKnownMovement > 4) addNewPunish("heuristic(aim)", 100);
            if (aggressiveAim > 8) addNewPunish("heuristic(aggressive)", 65);
            if (constantRotations > 2) addNewPunish("heuristic(constant)", 60);
            if (gcd > 2) addNewPunish("pattern(gcd)", 210);
            if (aggressivePatternI > 3 && aggressivePatternD > 3) addNewPunish("pattern(random)", 40);
            if (aggressivePatternI2 > 3 && aggressivePatternD2 > 3
                            && (aggressivePatternI2 + aggressivePatternD2) > 8 && targetsOnTheRow.size() < 2) {
                addNewPunish("pattern(snap)", 40);
            }
        }
        // player.sendMessage(aggressivePatternI + " " + aggressivePatternD);

        double punishValue = Math.floor(vl / 200);
        if (!punish.isEmpty() && vl > priorityData(priorityType()).getVlLimit()) {
            cancel(
                    "type: " + punish + ", bH: " + basicHeuristic
                                    + ", lH: " + lowHeuristic + ", rA: "
                                    + robotizedAmount + ", mKM: " + machineKnownMovement
                                    + ", aA: " + aggressiveAim + ", cR: " + constantRotations
                                    + ", gcd: " + gcd + ", aPI: " + aggressivePatternI
                                    + ", aPD: " + aggressivePatternD + ", aPI2: " + aggressivePatternI2
                                    + ", aPD2: " + aggressivePatternD2,
                    punishValue,
                    to
            );
            vl -= 10;
        }
        if (vl > 0) vl -= priorityData(priorityType()).getVlFader();
        if (vl > priorityData(priorityType()).getVlLimit()) vl -= 10;
        rayTraceAnalysis.clear();
        aimAnalysis.clear();

    }

    public void runDamage(Entity targetHandle) {
        attackHandle = new AttackHandle(targetHandle, System.currentTimeMillis());
    }

    public SpartanVector2F calculate(final SpartanVector3D from, final SpartanVector3D to) {
        SpartanVector3D diff = to.sub(from);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (Math.atan2(diff.z, diff.x) * 180.0F / PI) - 90.0F,
                pitch = (float) (-(Math.atan2(diff.y, distance) * 180.0F / PI));
        return new SpartanVector2F(yaw, pitch);
    }

    public void addNewPunish(String reason, float vlAdd) {
        vl += vlAdd / targetsOnTheRow.size();
        if (punish.isEmpty()) {
            punish = reason;
        } else {
            punish += ", " + reason;
        }
    }

    public PriorityType priorityType() {
        return PriorityType.CAREFUL;
    }

    public PriorityData priorityData(PriorityType priorityType) {
        switch (priorityType) {
            case AGGRESSIVE:
                return new PriorityData(200, 4);
            case CAREFUL:
                return new PriorityData(400, 5);
            case LENIENT:
                return new PriorityData(900, 6);
            case SILENT:
                return new PriorityData(1500, 8);
            default:
                return new PriorityData(400, 5);
        }
    }


}