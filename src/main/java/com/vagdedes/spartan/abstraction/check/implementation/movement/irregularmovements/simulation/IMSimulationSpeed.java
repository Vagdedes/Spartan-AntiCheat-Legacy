package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation;

import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.HardcodedDetection;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.math.RayLine;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2D;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2F;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.PI;

public class IMSimulationSpeed extends HardcodedDetection {

    public int ground = 0, air = 0;

    private boolean velo = false, sprintStatus = true;

    private static final double FRICTION = 0.91;
    private static final double ICE_FRICTION = 0.98;
    private static final double ACCELERATION_SPRINT = 0.026;
    private static final double ACCELERATION_BASIC = 0.02;
    private double ACCELERATION_MOTION = 0.0;
    private static final double[] JUMP
                    = new double[]{0.2806, 0.1726};
    private static final double[] JUMP_SPRINT =
                    new double[]{0.6132, 0.3601};
    private double predictX = 0, predictZ = 0;
    private double lastDx = 0, lastDz = 0;
    private double lastSpeed = 0;

    public IMSimulationSpeed(CheckRunner executor) {
        super(executor, "speed", true);
    }

    public final void run() {
        if (true) {
            return;
        }
        if (this.protocol().spartan != null) {
            SpartanProtocol p = this.protocol();
            boolean isOnGround = p.isOnGround();
            Location to = p.getLocation();
            Location from = this.protocol().getFromLocation();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            double rYaw = Math.toRadians(MathHelper.floor_float(to.getYaw()));
            RayLine yawLine = new RayLine(-Math.sin(rYaw), Math.cos(rYaw));
            RayLine moveLine = new RayLine(dx, dz);
            boolean isOnAngle = RayUtils.validRayLines(
                            yawLine, moveLine, 65.0);
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                {
                    this.ground(isOnGround);
                    if (this.air == 0) {
                        this.sprintStatus = false;
                        double limit = isOnAngle ? 0.2975 : 0.235;
                        double limitAccel = isOnAngle ? 0.15 : 0.12;
                        if (this.ground == 1 && lastSpeed > limitAccel)
                            this.ACCELERATION_MOTION = lastSpeed - limitAccel;
                        limit *= this.multiplex();
                        limit += this.ACCELERATION_MOTION;
                        if (speed < limit)
                            setPredict(dx, dz);
                        //p.bukkit.sendMessage("s: " + speed + " " + limit);
                        if (this.ACCELERATION_MOTION > 0)
                            this.ACCELERATION_MOTION -= 0.02;
                        if (this.ACCELERATION_MOTION < 0) this.ACCELERATION_MOTION = 0;
                    } else if (this.air == 1 || (this.air == 2 && !this.velo)) {
                        double lim;
                        if (RayUtils.validRayLines(
                                        yawLine, moveLine, 45) || sprintStatus)
                            lim = JUMP_SPRINT[air - 1];
                        else if (RayUtils.validRayLines(yawLine, moveLine, 60)) {
                            lim = JUMP[air - 1] + (JUMP_SPRINT[air - 1] / 2.0);
                        } else lim = JUMP[air - 1];
                        if (this.ACCELERATION_MOTION > 0.146)
                            lim += this.ACCELERATION_MOTION - 0.146;
                        lim *= this.multiplex();
                        if (speed < lim)
                            this.setPredict(dx, dz);
                        this.velocity(
                                        to.getY() - from.getY(),
                                        ACCELERATION_SPRINT * this.multiplex(),
                                        new SpartanVector2D(dx, dz)
                        );
                        if (p.isSprinting()
                                        && isOnAngle
                                        && this.air == 1) sprintStatus = true;
                    } else {
                        double pX = this.lastDx * 0.91f;
                        double pZ = this.lastDz * 0.91f;
                        double moveAngle = RayUtils.calculateRayLine(moveLine);
                        double accel = ACCELERATION_SPRINT * this.multiplex();
                        boolean point = this.isPointWall(to)
                                        || this.isPointWall(from)
                                        || this.isPointWall(
                                        to.clone().add(
                                                        -Math.sin(moveAngle) * 0.3, 0,
                                                        Math.cos(moveAngle) * 0.3));
                        if (point && speed < lastSpeed * 0.91f + 3.0E-3) {
                            if (getDeviantity(dx, pX) > accel) pX = dx;
                            if (getDeviantity(dz, pZ) > accel) pZ = dz;
                        }
                        boolean isCorrect =
                                        getDeviantity(dx, pX) < accel
                                        && getDeviantity(dz, pZ) < accel;
                        if (isCorrect)
                            this.setPredict(dx, dz);

                        if (!isCorrect) {
                            p.bukkit.sendMessage("d: " + getDeviantity(dx, pX) + " " + getDeviantity(dz, pZ));
                        }

                    }
                    p.bukkit.sendMessage("c: " + (dx == predictX && dz == predictZ) + " " + air);
                }
                this.lastDx = dx;
                this.lastDz = dz;
                this.lastSpeed = speed;
            }
        }
    }

    private double multiplex() {
        double m = 1.0;
        PotionEffect potionEffect = this.protocol().bukkit.getPotionEffect(PotionEffectType.SPEED);
        Location l = this.protocol().getLocation();
        if (potionEffect != null)
            m += (double) (potionEffect.getAmplifier() + 1) * 0.2;
        if (this.isOnIce(l))
            m *= 1.6;

        return m;
    }

    private static double getDeviantity(double a, double b) {
        return abs(abs(a) - abs(b));
    }

    private void velocity(double motion, double allowedDev, SpartanVector2D move) {
        PlayerVelocityEvent claimed = null;
        for (PlayerVelocityEvent velo : this.protocol().claimedVeloGravity) {
            if (this.veloIsValid(motion, velo.getVelocity().getY())) {
                if (getDeviantity(velo.getVelocity().getX(), move.x) <= allowedDev)
                    this.predictX = move.x;
                if (getDeviantity(velo.getVelocity().getZ(), move.y) <= allowedDev)
                    this.predictZ = move.y;
                claimed = velo;
                break;
            }
        }
        if (claimed != null) this.protocol().claimedVeloGravity.remove(claimed);
    }

    private boolean veloIsValid(double motion, double velo) {
        return abs(abs(motion) - abs(velo)) < 0.08;
    }

    public void teleport() {
        this.air = 0;
    }

    public void ground(boolean isOnGround) {
        this.air = isOnGround ? 0 : this.air + 1;
        this.ground = isOnGround ? this.ground + 1 : 0;
    }

    public void trident() {

    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private void setPredict(double x, double z) {
        this.predictX = x;
        this.predictZ = z;
    }

    private List<Entity> getEntitiesAroundPoint(Location location, double radius) {
        List<Entity> entities = new ArrayList<>();
        World world = location.getWorld();
        int smallX = MathHelper.floor_double((location.getX() - radius) / 16.0D);
        int bigX = MathHelper.floor_double((location.getX() + radius) / 16.0D);
        int smallZ = MathHelper.floor_double((location.getZ() - radius) / 16.0D);
        int bigZ = MathHelper.floor_double((location.getZ() + radius) / 16.0D);

        for (int x = smallX; x <= bigX; x++) {
            for (int z = smallZ; z <= bigZ; z++) {
                if (world.isChunkLoaded(x, z)) {
                    entities.addAll(Arrays.asList(world.getChunkAt(x, z).getEntities())); // Add all entities from this chunk to the list
                }
            }
        }
        entities.removeIf(entity -> SpartanLocation.distanceSquared(entity.getLocation(), location) > radius * radius);
        return entities;
    }

    private boolean isOnWater(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    SpartanBlock block = new SpartanLocation(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    ).getBlock();

                    Material m = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(),
                                    x + (double) dx * 0.3,
                                    y + (double) dy * 0.5,
                                    z + (double) dz * 0.3)
                    );

                    if (BlockUtils.isLiquidOrWaterLogged(block, true)
                            || BlockUtils.isLiquid(m)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnIce(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material m = this.protocol().packetWorld.getBlock(
                                    new Location(this.protocol().spartan.getWorld(),
                                                    x + (double) dx * 0.3,
                                                    y + (double) dy * 0.5,
                                                    z + (double) dz * 0.3)
                    );

                    if (BlockUtils.areIceBlocks(m)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    private boolean isPointWall(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = 0; dy <= 5; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                                    new Location(
                                                    this.protocol().spartan.getWorld(),
                                                    x + (double) dx * 0.4,
                                                    y + (double) dy * 0.5, z + (double) dz * 0.4));


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


    private boolean isOnSemi(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null &&
                            (BlockUtils.isSemiSolid(material))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isNearGlideBlocks(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null
                            && BlockUtils.isPowderSnow(material)
                            || BlockUtils.areHoneyBlocks(material)
                            || BlockUtils.areWebs(material)
                            || BlockUtils.areInteractiveBushes(material)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean atJumpModifyBlock(Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null && material
                            .toString().equalsIgnoreCase("SWEET_BERRY_BUSH")
                            || BlockUtils.isPowderSnow(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private boolean isNearClimbBlocks(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null
                            && BlockUtils.canClimb(material, false)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnSlime(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                    );

                    if (material != null
                            && (BlockUtils.areHoneyBlocks(material)
                            || BlockUtils.areBeds(material)
                            || BlockUtils.areSlimeBlocks(material))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSlimeWide(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -2; dz <= 2; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx, y + (double) dy, z + (double) dz)
                    );

                    if (material != null && (BlockUtils.areSlimeBlocks(material))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isOnSlimeHeight(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx, y + (double) dy, z + (double) dz)
                    );

                    if (material != null && (BlockUtils.areSlimeBlocks(material))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlocksOnTop(Location location) {
        double x = location.getX();
        double y = location.getY() - 0.1;
        double z = location.getZ();

        for (int dx = -2; dx <= 2; ++dx) {
            for (double dy = 1.0; dy <= 2.0; dy += 0.5) {
                for (int dz = -2; dz <= 2; ++dz) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(this.protocol().spartan.getWorld(), x + (double) dx * 0.3, y + dy, z + (double) dz * 0.3)
                    );

                    if (material != null && BlockUtils.isSolid(material)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnSolidGround(Location location) {
        double x = location.getX(),
                y = location.getY() - 0.1,
                z = location.getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Material material = this.protocol().packetWorld.getBlock(
                            new Location(
                                    this.protocol().spartan.getWorld(),
                                    x + dx,
                                    y + dy,
                                    z + dz)
                    );

                    if (material == null) {
                        continue;
                    }
                    if (BlockUtils.isSolid(material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static SpartanVector2F calculateVec2Vec(final Vector from, final Vector to) {
        final Vector diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (Math.atan2(diff.getZ(), diff.getX()) * 180.0F / PI) - 90.0F;
        final float pitch = (float) (-(Math.atan2(diff.getY(), distance) * 180.0F / PI));
        return new SpartanVector2F(yaw, pitch);
    }
}
