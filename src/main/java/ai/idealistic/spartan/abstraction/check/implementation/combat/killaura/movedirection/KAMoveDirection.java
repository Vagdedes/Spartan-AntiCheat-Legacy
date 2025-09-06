package ai.idealistic.spartan.abstraction.check.implementation.combat.killaura.movedirection;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.compatibility.necessary.protocollib.ProtocolLib;
import ai.idealistic.spartan.utils.java.ConcurrentList;
import ai.idealistic.spartan.utils.math.RayUtils;
import ai.idealistic.spartan.utils.minecraft.vector.CVector2F;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.PI;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KAMoveDirection extends CheckDetection {

    private final CheckDetection layer1, layer2;
    private AttackHandle attackHandle;
    private final List<AttackData> aimAnalysis;
    private float vl, vlL2;
    private String punish, punishL2;
    private double lastSwitchYaw;
    private final List<UUID> targetsOnTheRow;
    private final List<Double> rayTraceAnalysis;
    private int streak = 0;

    public KAMoveDirection(CheckRunner executor) {
        super(executor, null, null, null, true);
        this.layer1 = new ImplementedDetection(this, "move_direction_1", true,
                        DEFAULT_AVERAGE_TIME * 4L,
                        TIME_TO_NOTIFY,
                        TIME_TO_PREVENT,
                        TIME_TO_PUNISH);
        this.layer2 = new ImplementedDetection(this, "move_direction_2", true,
                        DEFAULT_AVERAGE_TIME * 4L,
                        TIME_TO_NOTIFY,
                        TIME_TO_PREVENT,
                        TIME_TO_PUNISH);
        this.aimAnalysis = new ConcurrentList<>();
        this.attackHandle = new AttackHandle(null, System.currentTimeMillis());

        this.vl = 0;
        this.vlL2 = 0;
        this.punish = "";
        this.punishL2 = "";
        this.targetsOnTheRow = new ConcurrentList<>();
        this.rayTraceAnalysis = new ConcurrentList<>();
    }

    public void run() {
        this.call(() -> {
            if (attackHandle.time + 3000 > System.currentTimeMillis()) {
                Entity target = attackHandle.target;

                if (target != null) {
                    Location targetLoc = ProtocolLib.getLocationOrNull(target);

                    if (targetLoc == null) {
                        return;
                    }
                    double bruteforceRayTrace = RayUtils.bruteforceRayTrace(this.protocol, target);
                    double diff = bruteforceRayTrace - 0.4;
                    double predict = RayUtils.castTo360(
                                    calculate(
                                                    this.protocol.getLocation().toVector(),
                                                    new ServerLocation(targetLoc).toVector()
                                    ).x
                    );
                    double yaw360 = RayUtils.castTo360(this.protocol.getLocation().getYaw());
                    rayTraceAnalysis.add(bruteforceRayTrace);
                    aimAnalysis.add(
                                    new AttackData(
                                                    Math.abs(predict - yaw360),
                                                    this.protocol.getLocation().getYaw(),
                                                    this.protocol.getLocation().getPitch(),
                                                    System.currentTimeMillis(),
                                                    ProtocolLib.getUUID(target)
                                    )
                    );
                    if (rayTraceAnalysis.size() >= 10) {
                        analyze();
                    }
                }
            }
        });
    }

    private void analyze() {
        targetsOnTheRow.clear();
        punish = "";
        punishL2 = "";

        List<Double> yaws = new ArrayList<>();
        double oldYawResult = aimAnalysis.get(0).yaw;
        double oldPitchResult = aimAnalysis.get(0).pitch;
        double oldYawChange = Math.abs(aimAnalysis.get(0).yaw - oldYawResult);
        double yawChangeFirst = Math.abs(aimAnalysis.get(0).yaw - aimAnalysis.get(1).yaw);

        int machineKnownMovement = 0,
                        constantRotations = 0, gcd = 0, aggressivePatternI = 0,
                        aggressivePatternD = 0, aggressivePatternI2 = 0, aggressivePatternD2 = 0,
                        robotizedAmount = 0, aggressiveAim = 0, infinitives = 0, randomizerFlaw = 0;

        for (AttackData value : aimAnalysis) {
            if (!targetsOnTheRow.contains(value.target))
                targetsOnTheRow.add(value.target);

            double yawChange = Math.abs(value.yaw - oldYawResult);
            double pitchChange = Math.abs(value.pitch - oldPitchResult);
            yaws.add(yawChange);

            if ((pitchChange > 1.5f || yawChange > 3.0f) && (value.pitch == 0 || value.pitch % 0.01f == 0)) {
                randomizerFlaw++;
            }

            double robotized = Math.abs(yawChange - yawChangeFirst);
            double diffBetweenYawChanges = yawChange - oldYawChange;
            double interpolation;

            if (robotized < 2 && yawChange > 2.5) robotizedAmount++;
            if (robotized < 0.99 && yawChange > 4) machineKnownMovement++;
            if (robotized < 0.02 && yawChange > 3) constantRotations++;
            if (robotized < 2 && yawChange > 3) aggressiveAim++;

            interpolation = RayUtils.scaleVal(yawChange / robotized, 2);
            if (Double.isInfinite(interpolation) && yawChange > 0) {
                infinitives++;
                if (infinitives > 1 && yawChange < 0.4) {
                    infinitives--;
                }
            }

            if (yawChange == 0.1 || pitchChange == 0.1) gcd++;
            if (yawChange == 0.01 || pitchChange == 0.01) gcd++;
            if ((diffBetweenYawChanges > 0.01 && diffBetweenYawChanges < 2)) aggressivePatternI++;
            if ((diffBetweenYawChanges < -0.01 && diffBetweenYawChanges > -2)) aggressivePatternD++;
            if (diffBetweenYawChanges > 2) aggressivePatternI2++;
            if (diffBetweenYawChanges < -2) aggressivePatternD2++;

            oldYawResult = value.yaw;
            oldPitchResult = value.pitch;
            oldYawChange = yawChange;
        }

        double yawAverage = 0;
        if (!yaws.isEmpty()) {
            double sum = 0;
            for (double d : yaws) {
                sum += d;
            }
            yawAverage = sum / yaws.size();
        }

        if (robotizedAmount > 8) addNewPunish("heuristic(sync)", 125);
        if (aggressiveAim > 8) addNewPunish("heuristic(aggressive)", 50);
        if (machineKnownMovement > 7) addNewPunish("heuristic(aim)", 100);
        if (constantRotations > 3) addNewPunish("heuristic(constant)", 65);
        if (randomizerFlaw > 0) addNewPunish("randomizer(flaw)", randomizerFlaw * 100);

        if (infinitives > 1 && Math.abs(yawAverage) > 3.2) {
            addNewPunishL2("heuristic(interpolation)", 55);
        }
        if (gcd > 0) addNewPunish("pattern(gcd)", 1000);
        if (aggressivePatternI > 3 && aggressivePatternD > 3) {
            addNewPunishL2("pattern(random)", 25);
        }
        if (aggressivePatternI2 > 3 && aggressivePatternD2 > 3
                        && (aggressivePatternI2 + aggressivePatternD2) > 8) {
            streak++;
            if (streak > 2) {
                addNewPunish("pattern(snap)", 55);
            }
        } else {
            streak = 0;
        }

        if (!punish.isEmpty() && vl > 400) {
            this.layer1.cancel(
                            "(layer1) type: " + punish + ", rA: "
                                            + robotizedAmount + ", mKM: " + machineKnownMovement
                                            + ", aA: " + aggressiveAim + ", cR: " + constantRotations
                                            + ", gcd: " + gcd + ", aPI: " + aggressivePatternI
                                            + ", aPD: " + aggressivePatternD + ", aPI2: " + aggressivePatternI2
                                            + ", aPD2: " + aggressivePatternD2 + ", rF: " + randomizerFlaw,
                            this.protocol.getFromLocation()
            );
            vl -= 10;
        }

        if (!punishL2.isEmpty() && vlL2 > 400) {
            this.layer2.cancel(
                            "(layer2) type: " + punishL2 + ", infs: " + infinitives,
                            this.protocol.getFromLocation()
            );
            vlL2 -= 65;
        }

        if (vl > 0) vl -= 5;
        if (vl > 400) vl -= 10;
        if (vlL2 > 0) vlL2 -= 5;
        if (vlL2 > 380) vlL2 -= 10;

        rayTraceAnalysis.clear();
        aimAnalysis.clear();
    }

    public void run(Entity targetHandle) {
        this.call(() -> {
            if (targetHandle instanceof Player
                            || targetHandle instanceof Zombie
                            || targetHandle instanceof Creeper
                            || targetHandle instanceof Skeleton) {
                attackHandle = new AttackHandle(targetHandle, System.currentTimeMillis());
            }
        });
    }

    private static CVector2F calculate(final Vector from, final Vector to) {
        Vector diff = to.subtract(from);
        double distance = Math.hypot(diff.getX(), diff.getZ());
        float yaw = (float) (Math.atan2(diff.getZ(), diff.getX()) * 180.0F / Math.PI) - 90.0F,
                        pitch = (float) (-(Math.atan2(diff.getY(), distance) * 180.0F / Math.PI));
        return new CVector2F(yaw, pitch);
    }

    private void addNewPunish(String reason, float vlAdd) {
        vl += vlAdd / targetsOnTheRow.size();
        if (punish.isEmpty()) {
            punish = reason;
        } else {
            punish += ", " + reason;
        }
    }

    private void addNewPunishL2(String reason, float vlAdd) {
        vlL2 += vlAdd;
        if (punishL2.isEmpty()) {
            punishL2 = reason;
        } else if (!punishL2.contains(reason)) {
            punishL2 += ", " + reason;
        }
    }

    private static PriorityType priorityType() {
        return PriorityType.CAREFUL;
    }

    private static PriorityData priorityData(PriorityType priorityType) {
        switch (priorityType) {
            case AGGRESSIVE:
                return new PriorityData(200, 4);
            case LENIENT:
                return new PriorityData(900, 6);
            case SILENT:
                return new PriorityData(1500, 8);
            default:
                return new PriorityData(400, 5);
        }
    }
}