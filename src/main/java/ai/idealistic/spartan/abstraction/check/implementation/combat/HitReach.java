package ai.idealistic.spartan.abstraction.check.implementation.combat;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.event.PlayerAttackEvent;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerBlock;
import ai.idealistic.spartan.functionality.server.MultiVersion;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.tracking.CheckConditions;
import ai.idealistic.spartan.listeners.bukkit.standalone.ChunksEvent;
import ai.idealistic.spartan.utils.java.ConcurrentList;
import ai.idealistic.spartan.utils.math.ClientMath;
import ai.idealistic.spartan.utils.math.OptifineMath;
import ai.idealistic.spartan.utils.math.RayUtils;
import ai.idealistic.spartan.utils.math.VanillaMath;
import ai.idealistic.spartan.utils.math.statistics.StatisticsMath;
import ai.idealistic.spartan.utils.minecraft.entity.AxisAlignedBB;
import ai.idealistic.spartan.utils.minecraft.entity.MovingObjectPosition;
import ai.idealistic.spartan.utils.minecraft.vector.Vec3;
import ai.idealistic.spartan.utils.minecraft.world.BoundingBoxUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class HitReach extends CheckRunner {

    private final CheckDetection
            hitboxRaytrace,
            throughblocks,
            simple;

    private final static boolean allowedVer = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14);
    private final List<Double> rayTraceAnalysis = new ConcurrentList<>();
    private int vl = 0;
    private static final boolean[] BOOLEANS = {true, false};
    private static final double BASE_REACH_DIST = 3.001, HIT_BOX_VAL = 10.0;
    private double hitBoxVL = -1;
    private double reachVl = 0;
    private double blocksVl = 0;
    private final OptifineMath optifineMath = new OptifineMath();
    private final VanillaMath vanillaMath = new VanillaMath();

    public HitReach(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        hitboxRaytrace = new ImplementedDetection(this, null, null, "hitbox_raytrace", true,
                CheckDetection.DEFAULT_AVERAGE_TIME * 4L,
                CheckDetection.TIME_TO_NOTIFY,
                CheckDetection.TIME_TO_PREVENT,
                CheckDetection.TIME_TO_PUNISH);
        simple = new ImplementedDetection(this, null, null, "simple", true,
                CheckDetection.DEFAULT_AVERAGE_TIME * 4L,
                CheckDetection.TIME_TO_NOTIFY,
                CheckDetection.TIME_TO_PREVENT,
                CheckDetection.TIME_TO_PUNISH);
        throughblocks = new ImplementedDetection(this, null, null, "throughblocks", true,
                CheckDetection.DEFAULT_AVERAGE_TIME * 4L,
                CheckDetection.TIME_TO_NOTIFY,
                CheckDetection.TIME_TO_PREVENT,
                CheckDetection.TIME_TO_PUNISH);
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerAttackEvent) {
            PlayerAttackEvent event = (PlayerAttackEvent) object;
            if (!(event.target instanceof Player)) return;
            if (event.target.getVehicle() != null || this.protocol.getVehicle() != null) return;

            if (this.protocol.packetsEnabled()) {
                if (CheckConditions.canCheckCombat(this.protocol, PluginBase.getProtocol((Player) event.target).bukkit())
                        && !this.protocol.getGameMode().equals(GameMode.CREATIVE)) {
                    PlayerProtocol target = PluginBase.getProtocol((Player) event.target);
                    List<Location> h = target.getPositionHistory();
                    Set<AxisAlignedBB> cachedBlocks = new HashSet<>();
                    Location hardcodedTargetLoc = target.getLocation().clone();
                    Location hardcodedPlayerLoc = this.protocol.getLocation().clone();

                    // EXTENSION: Calculate Attacker Momentum for Dynamic Reach Scaling
                    Location lf = this.protocol.getFromLocation();
                    double horizontalSpeed = Math.sqrt(Math.pow(hardcodedPlayerLoc.getX() - lf.getX(), 2) +
                            Math.pow(hardcodedPlayerLoc.getZ() - lf.getZ(), 2));

                    // Allow up to 0.5 extra blocks of reach per tick of forward momentum (absorbs the sprint-desync)
                    double dynamicReach = BASE_REACH_DIST + (horizontalSpeed * 0.85);

                    if (getDistance(hardcodedPlayerLoc, hardcodedTargetLoc) > 0.5) {
                        final Location t = target.getLocation();
                        for (int dx = -3; dx <= 3; ++dx) {
                            for (int dy = -3; dy <= 3; ++dy) {
                                for (int dz = -3; dz <= 3; ++dz) {
                                    final Location l = new Location(
                                            target.bukkit().getWorld(),
                                            t.getX() + (double) dx,
                                            t.getY() + (double) dy,
                                            t.getZ() + (double) dz
                                    );
                                    final Block block = ChunksEvent.getBlockAsync(l);

                                    if (block == null) {
                                        continue;
                                    }
                                    final Material material = new ServerBlock(block).getType();
                                    final List<AxisAlignedBB> bb = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                                            ? BoundingBoxUtil.getBoundingBoxes(block)
                                            : BoundingBoxUtil.getBoundingBoxesLegacy(block);
                                    if (material.isSolid() && bb != null) {
                                        cachedBlocks.addAll(bb);
                                    }
                                }
                            }
                        }
                    }

                    final boolean is1_8 = this.protocol.isUsingVersion(MultiVersion.MCVersion.V1_8);
                    // EXTENSION: Expanded baseline hitbox to safely blanket StrictMath & Pose transitions
                    final float hitbox = (is1_8) ? 0.45F : 0.35F;

                    AtomicReference<Double> minDistance = new AtomicReference<>(Double.MAX_VALUE);
                    Set<Double> minBlockDistanceSet = new HashSet<>();

                    {
                        for (boolean fastMath : BOOLEANS) {
                            final ClientMath buildSpeed = fastMath ? optifineMath : vanillaMath;
                            final Location l = protocol.getLocation();
                            Vec3[] possibleEyeRotation = {
                                    getVectorForRotation(l.getYaw(), l.getPitch(), buildSpeed),
                                    getVectorForRotation(lf.getYaw(), lf.getPitch(), buildSpeed),
                                    getVectorForRotation(lf.getYaw(), lf.getPitch(), buildSpeed),
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

                                    List<Location> positions = new LinkedList<>(h);
                                    positions.add(target.getLocation());
                                    positions.add(target.getFromLocation());
                                    positions.add(target.bukkit().getLocation().clone());

                                    for (Location position : positions) {
                                        AxisAlignedBB axisAlignedBB = new AxisAlignedBB(
                                                position.getX() - hitbox, position.getY() - 0.1f, position.getZ() - hitbox,
                                                position.getX() + hitbox, position.getY() + 1.9f, position.getZ() + hitbox
                                        );

                                        MovingObjectPosition intercept = axisAlignedBB.calculateIntercept(eyePos, endReachRay);
                                        if (intercept != null) {
                                            // EXTENSION: StrictMath tolerance subtraction (0.05)
                                            double range = Math.max(0, intercept.hitVec.distanceTo(eyePos) - 0.05);
                                            if (range < minDistance.get()) {
                                                minDistance.set(range);
                                            }
                                        }
                                    }

                                    double minBlockDist = Double.MAX_VALUE;
                                    for (AxisAlignedBB block : cachedBlocks) {
                                        Vec3 s = new Vec3(
                                                l.getX(),
                                                l.getY() + getBlockEyeHeight(sneaking),
                                                l.getZ()
                                        );
                                        Vec3 e = s.addVector(
                                                eyeRotation.xCoord * 6.0,
                                                eyeRotation.yCoord * 6.0,
                                                eyeRotation.zCoord * 6.0
                                        );

                                        MovingObjectPosition intercept = block.calculateIntercept(s, e);
                                        double v = (intercept != null) ? intercept.hitVec.distanceTo(eyePos) : 0;
                                        if (v < (minDistance.get()) && v < minBlockDist
                                                && intercept != null && v < 6) {
                                            minBlockDist = v;
                                        }
                                    }
                                    minBlockDistanceSet.add(minBlockDist);
                                }
                            }
                        }

                        boolean flagged = false;
                        if (minDistance.get() > HIT_BOX_VAL) {
                            flagged = true;
                            if (++hitBoxVL > 3D
                                    && !this.protocol.isBedrockPlayer()) {
                                this.hitboxRaytrace.cancel("Missing HitBox of " + target.bukkit().getName());

                                if (this.hitboxRaytrace.prevent()) {
                                    event.setCancelled(true);
                                }
                            }
                        } else {
                            hitBoxVL = Math.max(hitBoxVL - 5D, 0);
                        }

                        // APPLIED EXTENSION: Use dynamicReach instead of static REACH_DIST
                        if (minDistance.get() > dynamicReach && minDistance.get() < HIT_BOX_VAL) {
                            flagged = true;
                            if (reachVl++ > 2.5D) {
                                hitboxRaytrace.cancel("Distance " + RayUtils.scaleVal(minDistance.get(), 3) + " (Max: " + RayUtils.scaleVal(dynamicReach, 3) + ")");

                                if (hitboxRaytrace.prevent()) {
                                    event.setCancelled(true);
                                }
                            }
                        } else if (minDistance.get() < dynamicReach && reachVl > 0) {
                            reachVl -= 0.25;
                        }

                        {
                            double dev = (StatisticsMath.getMax(minBlockDistanceSet) + 0.03 + 3e-3) - minDistance.get();

                            // EXTENSION: Softened the hardcoded block threshold slightly to account for complex 1.14+ geometry
                            if (dev < -0.85 && RayUtils.scaleVal(Math.abs(dev), 6) < 10) {
                                blocksVl++;
                                throughblocks.cancel("Hitting through block (dev: "
                                        + RayUtils.scaleVal(Math.abs(dev), 6) + ")");

                                if (throughblocks.prevent()) {
                                    event.setCancelled(true);
                                }
                            } else if (blocksVl > 0) {
                                blocksVl -= 0.5;
                            }
                        }
                    }
                }
            } else {
                LivingEntity target = event.target;
                if (CheckConditions.canCheckCombat(this.protocol, target)
                        && !this.protocol.getGameMode().equals(GameMode.CREATIVE)) {
                    boolean sleeping = protocol.bukkit().isSleeping();

                    PluginBase.runTask(target, () -> {
                        double bruteforceRayTrace = RayUtils.bruteforceRayTrace(this.protocol, target, sleeping);
                        rayTraceAnalysis.add(bruteforceRayTrace);
                        if (rayTraceAnalysis.size() >= 10) {
                            analyze(this.protocol.getLocation(), bruteforceRayTrace);
                        }
                    });
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

    private float getEyeHeight(boolean sneaking) {
        float eyeHeight = 1.62F;
        if (sneaking) {
            eyeHeight -= 0.08F;
        }
        if (allowedVer && this.protocol.bukkit().getPose().equals(Pose.SWIMMING)) {
            eyeHeight -= 1.0f;
        }
        return eyeHeight;
    }

    private float getBlockEyeHeight(boolean sneaking) {
        float eyeHeight;
        if (allowedVer && this.protocol.bukkit().getPlayer().getPose().equals(Pose.SWIMMING)) {
            eyeHeight = 0.4f;
        } else {
            eyeHeight = 1.62F;
            if (sneaking) {
                eyeHeight -= 0.35F;
            }
        }
        return eyeHeight;
    }

    private static double getDistance(Location loc1, Location loc2) {
        double deltaX = loc1.getX() - loc2.getX();
        double deltaY = loc1.getY() - loc2.getY();
        double deltaZ = loc1.getZ() - loc2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
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
            this.simple.cancel(
                    punish + ", hitbox-size-analysis: "
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
        return CheckConditions.canCheckCombat(this.protocol);
    }
}