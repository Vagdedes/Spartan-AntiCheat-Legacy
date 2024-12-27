package com.vagdedes.spartan.abstraction.check.implementation.combat.hitreach;

import com.vagdedes.spartan.abstraction.check.CheckDetection;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.definition.ImplementedProbabilityDetection;
import com.vagdedes.spartan.abstraction.event.PlayerAttackEvent;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.ClientMath;
import com.vagdedes.spartan.utils.math.OptifineMath;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.math.VanillaMath;
import com.vagdedes.spartan.utils.minecraft.entity.AxisAlignedBB;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.entity.MovingObjectPosition;
import com.vagdedes.spartan.utils.minecraft.vector.Vec3;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class HitReach extends CheckRunner {

    private final CheckDetection
                    hitboxRaytrace,
                    simple;
    private final List<Double> rayTraceAnalysis = new ArrayList<>();
    private int vl = 0;
    private static final boolean[] BOOLEANS = {true, false};
    private static final double REACH_DIST = 3.001, HIT_BOX_VAL = 10.0;
    private double hitBoxVL = -1;
    private double reachVl = 0;
    private final OptifineMath optifineMath = new OptifineMath();
    private final VanillaMath vanillaMath = new VanillaMath();

    public HitReach(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        hitboxRaytrace = new ImplementedProbabilityDetection(this, "hitbox_raytrace", true);
        simple = new ImplementedProbabilityDetection(this, "simple", true);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        PlayerAttackEvent event = (PlayerAttackEvent) object;
        if (!(event.target instanceof Player)) return;
        if (event.target.getVehicle() != null || this.protocol.spartan.getVehicle() != null) return;
        if (this.protocol.packetsEnabled()) {
            if (CombatUtils.canCheck(this.protocol, SpartanBukkit.getProtocol((Player) event.target).bukkit())
                            && !this.protocol.bukkit().getGameMode().equals(GameMode.CREATIVE)) {
                SpartanProtocol target = SpartanBukkit.getProtocol((Player) event.target);
                List<Location> h = target.getPositionHistory();
                boolean is1_8 = this.protocol.isUsingVersion(MultiVersion.MCVersion.V1_8);
                float hitbox = (is1_8) ? 0.4F : 0.3F;
                AtomicReference<Double> minDistance = new AtomicReference<>(Double.MAX_VALUE);
                {
                    for (boolean fastMath : BOOLEANS) {
                        ClientMath clientMath = fastMath ? optifineMath : vanillaMath;

                        Location l = this.protocol.getLocation();
                        Location lF = this.protocol.getFromLocation();
                        Vec3[] possibleEyeRotation = {
                                        getVectorForRotation(l.getYaw(), l.getPitch(), clientMath),
                                        getVectorForRotation(lF.getYaw(), lF.getPitch(), clientMath),
                                        getVectorForRotation(lF.getYaw(), l.getPitch(), clientMath),
                        };

                        for (boolean sneaking : BOOLEANS) {
                            for (Vec3 eyeRotation : possibleEyeRotation) {
                                Vec3 eyePos = new Vec3(
                                                l.getX(),
                                                l.getY() + getEyeHeight(sneaking),
                                                l.getZ()
                                );
                                Vec3 endReachRay = eyePos.addVector(
                                                eyeRotation.xCoord * 6.0D,
                                                eyeRotation.yCoord * 6.0D,
                                                eyeRotation.zCoord * 6.0D
                                );
                                List<Location> positions = new ArrayList<>(h);
                                positions.add(target.getLocation());
                                positions.add(target.getFromLocation());
                                for (Location position : positions) {

                                    AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                                                    position.getX() - hitbox, position.getY() - 0.1f, position.getZ() - hitbox,
                                                    position.getX() + hitbox, position.getY() + 1.9f, position.getZ() + hitbox
                                    );

                                    MovingObjectPosition intercept = axisAlignedBB.calculateIntercept(eyePos, endReachRay);

                                    if (intercept != null) {
                                        double range = intercept.hitVec.distanceTo(eyePos);
                                        if (range < minDistance.get()) {
                                            minDistance.set(range);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (minDistance.get() > HIT_BOX_VAL) {
                        if (++hitBoxVL > 3D) {
                            hitboxRaytrace.cancel("Missing HitBox of " + target.bukkit().getName());
                        }
                    } else {
                        hitBoxVL = Math.max(hitBoxVL - 5D, 0);
                    }

                    if (minDistance.get() > REACH_DIST && minDistance.get() < HIT_BOX_VAL) {
                        reachVl++;
                        if (reachVl > 2.5D) {
                            hitboxRaytrace.cancel("Reach " + minDistance.get());
                        }
                    } else if (minDistance.get() < REACH_DIST && reachVl > 0) {
                        reachVl -= 0.05;
                    }
                }
            }
        } else {
            LivingEntity target = event.target;
            if (CombatUtils.canCheck(this.protocol, target)
                            && !this.protocol.bukkit().getGameMode().equals(GameMode.CREATIVE)) {
                double bruteforceRayTrace = RayUtils.bruteforceRayTrace(this.protocol, target),
                                diff = bruteforceRayTrace - 0.4;
                rayTraceAnalysis.add(bruteforceRayTrace);
                if (rayTraceAnalysis.size() >= 10) {
                    analyze(this.protocol.getLocation(), bruteforceRayTrace);
                }
            }
        }
    }
    private static Vec3 getVectorForRotation(float yaw, float pitch, ClientMath clientMath) {
        float f = clientMath.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = clientMath.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -clientMath.cos(-pitch * 0.017453292F);
        float f3 = clientMath.sin(-pitch * 0.017453292F);

        return new Vec3(f1 * f2, f3, f * f2);
    }
    private static float getEyeHeight(boolean sneaking) {
        float eyeHeight = 1.62F;

        if (sneaking) {
            eyeHeight -= 0.08F;
        }

        return eyeHeight;
    }
    private void analyze(Location to, double bruteForce) {
        String punish = null;
        double globalValue = 0;

        for (Double value : rayTraceAnalysis) {
            globalValue += value;
        }
        double result = globalValue / rayTraceAnalysis.size();

        if (result > (this.protocol.packetsEnabled() ? 0.43 : 0.5)) punish = "simple";
        int percent = (int) ((result / 0.4) * 100);
        if (punish != null) {
            vl += 100;
            this.simple.cancel( // ATTENTION: Add more detections to the check if you write more definitions for the 'punish' String field
                            "type: " + punish + ", hitbox-size-analysis: "
                                            + result + ", hitbox-size-last: " + bruteForce + ", percent-of-deviation: "
                                            + percent + "%, reach: " + RayUtils.scaleVal((result / 0.4) * 3, 2) + " blocks",
                            to
            );
        } else {
            if (vl > 0) vl -= 50;
        }
        rayTraceAnalysis.clear();

    }

    @Override
    protected boolean canRun() {
        return CombatUtils.canCheck(this.protocol);
    }
}