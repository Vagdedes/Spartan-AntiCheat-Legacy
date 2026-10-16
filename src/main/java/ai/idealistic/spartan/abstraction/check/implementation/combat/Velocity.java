package ai.idealistic.spartan.abstraction.check.implementation.combat;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.functionality.tracking.CheckConditions;
import ai.idealistic.spartan.utils.math.RayUtils;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public class Velocity extends CheckRunner {

    private final CheckDetection vertical, horizontal;

    public Velocity(CheckEnums.HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.vertical = new ImplementedDetection(this, null, Check.DetectionType.PACKETS, "vertical", true,
                CheckDetection.DEFAULT_AVERAGE_TIME * 4L,
                CheckDetection.TIME_TO_NOTIFY,
                CheckDetection.TIME_TO_PREVENT,
                CheckDetection.TIME_TO_PUNISH);
        this.horizontal = new ImplementedDetection(this, null, Check.DetectionType.PACKETS, "horizontal", true,
                CheckDetection.DEFAULT_AVERAGE_TIME * 4L,
                CheckDetection.TIME_TO_NOTIFY,
                CheckDetection.TIME_TO_PREVENT,
                CheckDetection.TIME_TO_PUNISH);
    }

    private final double[] jumpReset = new double[]{0.248136, 0.3332};

    private float vl = 0;
    private double mostCloseYMotion = 1.0, mostCloseYDifference = Double.MAX_VALUE;
    private int moveTicks = 0; // Decoupled from transactions
    private Vector velocity = null;

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent event = (PlayerVelocityEvent) object;

            Location[] locationsToCheck = {
                    this.protocol.getLocation().clone().add(event.getVelocity()),
                    this.protocol.getLocation().clone().add(event.getVelocity()).add(0, 1, 0)
            };
            boolean allClear = true;
            for (Location loc : locationsToCheck) {
                if (isPointWall(loc)) {
                    allClear = false;
                    break;
                }
            }
            if (allClear) {
                this.velocity = event.getVelocity();
                this.mostCloseYMotion = 1.0;
                this.mostCloseYDifference = Double.MAX_VALUE;
                this.moveTicks = 0; // Reset raw tick counter
            }

            if (vl > 0) vl -= 5;
        } else if (object instanceof PlayerMoveEvent) {
            if (this.horizontal.canCall() || this.vertical.canCall()) {
                PlayerMoveEvent event = (PlayerMoveEvent) object;
                Location from = event.getFrom();
                Location to = event.getTo();

                double x = to.getX() - from.getX();
                double y = to.getY() - from.getY();
                double z = to.getZ() - from.getZ();

                if (velocity != null) {
                    this.moveTicks++;

                    double absY = abs(y);
                    double absVeloY = abs(velocity.getY());

                    double yDifference = abs(absY - absVeloY);

                    if (yDifference < mostCloseYDifference) {
                        mostCloseYDifference = yDifference;
                        mostCloseYMotion = y;
                    }

                    // Calculate Percentage Yield for Vertical Reduce cheats
                    double yieldY = (absVeloY > 0) ? (absY / absVeloY) : 1.0;

                    if (yieldY > 0.98 || (absY > 0 && abs(absY - absVeloY) < 0.0001)) {
                        // Player accepted vertical velocity. Now check horizontal.
                        double xDiff = x - velocity.getX();
                        double zDiff = z - velocity.getZ();
                        double total = abs(xDiff) + abs(zDiff);

                        // Dynamic tolerance based on knockback severity (absorbs explosive kb + strafing)
                        double horizontalMagnitude = Math.sqrt((velocity.getX() * velocity.getX()) + (velocity.getZ() * velocity.getZ()));
                        double dynamicTolerance = Math.max(0.08, horizontalMagnitude * 0.35);

                        if (!this.protocol.isOnGround() && !this.protocol.isOnGroundFrom()) {
                            if (total > dynamicTolerance) {
                                this.flag("velocity manipulation [xz] (air deviation: " + r(total) + " > " + r(dynamicTolerance) + ")", 14, horizontal);
                            }
                        } else if (total > dynamicTolerance * 2.5) {
                            this.flag("velocity manipulation [xz] (ground deviation: " + r(total) + ")", 30, horizontal);
                        }

                        this.velocity = null;
                        this.moveTicks = 0;

                    } else if (this.moveTicks > 4) { // Timeout decoupled from transactions
                        // Check if they completely ignored it, or partially reduced it
                        if (yieldY < 0.98 && yieldY > 0.01 && this.velocity.getY() > 0.1) {
                            this.flag("velocity reduce [y] (yield: " + r(yieldY * 100) + "%)", 20, vertical);
                        } else if (this.velocity.getY() != 0.003) {
                            this.flag("velocity manipulation [y] (motion: " + r(this.mostCloseYMotion)
                                    + ", velocity: " + r(this.velocity.getY()) + ")", this.isJumpReset(this.mostCloseYMotion) ? 12 : 25, vertical);
                        }
                        this.velocity = null;
                    }
                }
            }
        }
    }

    private void flag(String reason, double vl, CheckDetection e) {
        e.call(() -> {
            this.vl += (float) vl;
            if (this.vl > 60) {
                e.cancel(reason);
                this.vl = 50;
            }
        });
    }

    private boolean isJumpReset(double v) {
        for (double d : this.jumpReset) {
            if (d == r(v)) return true;
        }
        return false;
    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private static double r(double v) {
        return RayUtils.scaleVal(v, 6);
    }

    private boolean isPointWall(Location location) {
        double x = location.getX();
        double y = location.getY() + 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = new ServerLocation(
                            new Location(location.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.3, z + (double) dz * 0.3)
                    ).getBlock(this.protocol).getTypeOrNull();

                    if (material != null &&
                            (BlockUtils.isSemiSolid(material)
                                    || BlockUtils.isSolid(material)
                                    || material.toString().contains("GRASS"))
                            || BlockUtils.isLiquid(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean canRun() {
        return this.protocol.getVehicle() == null
                && !this.protocol.getEnvironment().isSlime()
                && !this.protocol.getEnvironment().isSlimeWide()
                && !this.protocol.getEnvironment().isSlimeHeight()
                && !this.protocol.getEnvironment().isLiquid() // Exempt liquids (drastically dampens KB naturally)
                && !RayUtils.onBlock(this.protocol, this.protocol.getLocationOrVehicle(), BlockUtils.web) // Exempt webs
                && CheckConditions.canCheckCombat(this.protocol)
                && !this.protocol.isOutsideOfTheBorder(-1.0);
    }

}