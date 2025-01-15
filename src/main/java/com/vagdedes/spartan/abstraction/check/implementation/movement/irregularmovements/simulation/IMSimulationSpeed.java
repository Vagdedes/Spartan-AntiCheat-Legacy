package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.simulation;

import com.vagdedes.spartan.abstraction.check.Check;
import com.vagdedes.spartan.abstraction.check.CheckRunner;
import com.vagdedes.spartan.abstraction.check.ProbabilityDetection;
import com.vagdedes.spartan.abstraction.protocol.ExtendedPotionEffect;
import com.vagdedes.spartan.abstraction.protocol.PlayerTrackers;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.math.RayLine;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.EntityUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2D;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.potion.PotionEffectType;

public class IMSimulationSpeed extends ProbabilityDetection {

    private double vl = 0, vlCount = 0;

    private int ground = 0, air = 0, action = 0, vhact = 0;
    private boolean velo = false, sprintStatus = true;
    private static final double ACCELERATION_SPRINT = 0.026;
    private static final double ACCELERATION_BASIC = 0.02;
    private static final double ACCELERATION_ACTION = 0.0073;
    private double ACCELERATION_MOTION = 0.0, ACCELERATION_VELOCITY = 0.0;
    private static final double[] JUMP
            = new double[]{0.2806, 0.1726};
    private static final double[] JUMP_SPRINT =
            new double[]{0.6132, 0.3601};
    private double predictX = 0, predictZ = 0;
    private double lastDx = 0, lastDz = 0;
    private double lastSpeed = 0;
    private int nearSlimeStatus = 0, slimeBoostApply = 0, tridentBoostApply = 0;
    private boolean removePistonTick = false;

    public IMSimulationSpeed(CheckRunner executor) {
        super(executor, null, Check.DetectionType.PACKETS, "speed", true);
    }

    public void run() {
        this.call(() -> {
            SpartanProtocol p = this.protocol;
            boolean isOnGround = (p.spartan.getVehicle() != null)
                    ? p.spartan.getVehicle().isOnGround() : p.isOnGround();
            Location to = p.getLocation();
            Location from = this.protocol.getFromLocation();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            double rYaw = Math.toRadians(MathHelper.floor_float(to.getYaw()));
            RayLine yawLine = new RayLine(-Math.sin(rYaw), Math.cos(rYaw));
            RayLine moveLine = new RayLine(dx, dz);
            boolean isOnAngle = RayUtils.validRayLines(
                    yawLine, moveLine, 46.0);
            boolean isOnAngle80 = RayUtils.validRayLines(
                    yawLine, moveLine, 80.0);
            boolean flagged = false;
            if (!this.protocol.useItemPacket) this.action = 0;
            double atr = Attributes.getAmount(this.protocol, Attributes.GENERIC_MOVEMENT_SPEED) * 1.2;
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                {
                    this.ground(isOnGround);
                    if (this.air == 0) {
                        this.sprintStatus = false;
                        this.velo = false;
                        double limit = isOnAngle ? 0.2975 : (isOnAngle80) ? 0.289 : 0.235;
                        limit = (this.action > 6 ? 0.061 : this.action > 3 ? 0.09 : this.action > 1 ? 0.17 : limit);
                        double limitAccel = RayUtils.validRayLines(
                                yawLine, moveLine, 65.0) ? 0.15 : 0.12 + atr;
                        if (limit < (limit + atr) * this.multiplex())
                            this.ACCELERATION_MOTION = (((limit + atr) * this.multiplex()) - limit) * 0.5;
                        if (this.ground == 1 && lastSpeed > limitAccel)
                            this.ACCELERATION_MOTION = lastSpeed - limitAccel * 0.91;
                        limit += atr;
                        limit *= this.multiplex();
                        limit += this.ACCELERATION_MOTION;
                        if (this.protocol.getComponentXZ().pistonTick && speed > limit) {
                            limit += 1.0;
                            this.ACCELERATION_MOTION += 1.0;
                            this.removePistonTick = true;
                        }
                        if (speed < limit) this.setPredict(dx, dz);
                        if (this.ACCELERATION_MOTION > 0)
                            this.ACCELERATION_MOTION -= 0.02;
                        if (this.ACCELERATION_MOTION < 0) this.ACCELERATION_MOTION = 0;
                    } else if (this.air == 1 || (this.air == 2 && !this.velo)) {
                        double lim;
                        if (RayUtils.validRayLines(
                                yawLine, moveLine, 46) || sprintStatus)
                            lim = JUMP_SPRINT[air - 1];
                        else if (RayUtils.validRayLines(yawLine, moveLine, 65)) {
                            lim = JUMP[air - 1] + (JUMP_SPRINT[air - 1] / 2.0);
                        } else lim = JUMP[air - 1];
                        if (this.ACCELERATION_MOTION > 0.146)
                            lim += this.ACCELERATION_MOTION - 0.146;
                        lim += atr;
                        lim *= this.multiplex();
                        if (this.action > 0) lim /= 1.2;
                        if (this.protocol.getComponentXZ().pistonTick) lim += 1.0;
                        if (speed < lim + this.ACCELERATION_VELOCITY)
                            this.setPredict(dx, dz);
                        else this.setPredict(-Math.sin(Math.toRadians(to.getYaw())) * lim,
                                Math.cos(Math.toRadians(to.getYaw())) * lim);
                        if (p.isSprinting()
                                && isOnAngle
                                && this.air == 1) sprintStatus = true;
                        this.ACCELERATION_VELOCITY /= 1.3;
                    } else {
                        double pX = this.lastDx * 0.91f;
                        double pZ = this.lastDz * 0.91f;
                        this.setPredict(pX, pZ);
                        double speedAccel = Math.sqrt(getDeviantity(dx, pX) * getDeviantity(dx, pX) + getDeviantity(dz, pZ) * getDeviantity(dz, pZ));
                        double moveAngle = RayUtils.calculateRayLine(moveLine);
                        double accel = ((this.action > 2)
                                ? ACCELERATION_ACTION : (this.action > 1) ?
                                ACCELERATION_BASIC : ACCELERATION_SPRINT) * this.multiplex();
                        boolean nearbyEntities = false;
                        for (Entity entity : this.protocol.spartan.getNearbyEntities(2.4)) {
                            if (entity.getUniqueId() != this.protocol.getUUID()) {
                                nearbyEntities = true;
                                break;
                            }
                        }
                        boolean point = this.isPointWall(to, from, to.clone().add(
                                -Math.sin(moveAngle) * 0.3, 0,
                                Math.cos(moveAngle) * 0.3))
                                || (nearbyEntities);
                        if (point && speed < lastSpeed * 1.2 + (nearbyEntities ? accel + 0.2 : 0.03)) {
                            if (getDeviantity(dx, pX) > accel) pX = dx;
                            if (getDeviantity(dz, pZ) > accel) pZ = dz;
                        }
                        boolean isCorrect =
                                getDeviantity(dx, pX) < accel
                                        && getDeviantity(dz, pZ) < accel;
                        if (isCorrect)
                            this.setPredict(dx, dz);
                        else this.setPredict(
                                -Math.sin(Math.toRadians(to.getYaw())) * ACCELERATION_BASIC,
                                Math.cos(Math.toRadians(to.getYaw())) * ACCELERATION_BASIC);

                    }
                    this.velocity(
                            to.getY() - from.getY(),
                            ACCELERATION_SPRINT * (1.5) * this.multiplex(),
                            new SpartanVector2D(dx, dz)
                    );
                    //protocol().bukkit.sendMessage("i: " + this.protocol.getComponentXZ().pistonTick + " " + this.removePistonTick);
                    if ((this.isOnSlimeWide(to) || this.isOnSlimeWide(from)))
                        this.nearSlimeStatus = 4;
                    if (!(dx == predictX && dz == predictZ)) {
                        double pr = Math.sqrt(predictX * predictX + predictZ * predictZ);
                        double d = getDeviantity(pr, speed);
                        if (d <= 1.01 && d > 0.1 && this.protocol.getComponentXZ().pistonTick) {
                            this.setPredict(dx, dz);
                            this.removePistonTick = true;
                        }
                        if (this.protocol.getComponentXZ().pistonTick && this.nearSlimeStatus > 0) {
                            this.nearSlimeStatus = 0;
                            this.slimeBoostApply = 3;
                            this.removePistonTick = true;
                        }
                        if (d >= 0.04 && d <= 2.2 && this.slimeBoostApply > 0) {
                            this.slimeBoostApply--;
                            this.setPredict(dx, dz);
                        }
                        if (d >= 0.04 && d <= 3.1 && this.tridentBoostApply > 0) {
                            this.tridentBoostApply--;
                            this.setPredict(dx, dz);
                        }
                        if (d >= 0.02 && d <= 1.7 && this.protocol.getComponentXZ().explosionTick) {
                            this.protocol.getComponentXZ().explosionTick = false;
                            this.setPredict(dx, dz);
                        }
                        if (this.protocol.getComponentXZ().pistonTick
                                && getDeviantity(pr, speed) > 0.1 && getDeviantity(pr, speed) <= 1.0) {
                            this.setPredict(dx, dz);
                            this.removePistonTick = true;
                        }
                        if (d >= 0.01 && d <= 4.3
                                && (this.protocol.spartan.trackers.has(PlayerTrackers.TrackerType.VEHICLE, "vh_tp")
                                || this.vhact > 0)) {
                            this.setPredict(dx, dz);
                        }
                        if (d > 0.005 && this.protocol.spartan.getVehicle() != null) {
                            Entity v = this.protocol.spartan.getVehicle();
                            boolean isJumpingEntity = EntityUtils.abstractHorseClass
                                    ? v instanceof AbstractHorse
                                    : v instanceof Horse;
                            if (d < 2.2 && v instanceof Boat && this.isOnIce(v.getLocation().add(0, -0.45, 0))) {
                                this.setPredict(dx, dz);
                            } else if (d < 0.5 && v instanceof Boat) {
                                this.setPredict(dx, dz);
                            } else if (d < (isOnGround ? 0.63 : 0.2) * this.multiplex() && isJumpingEntity) {
                                this.setPredict(dx, dz);
                            } else if (d < 0.6 && v instanceof Minecart) {
                                this.setPredict(dx, dz);
                            } else if (d < 0.3 * this.multiplex()) {
                                this.setPredict(dx, dz);
                            }
                        }
                        pr = Math.sqrt(predictX * predictX + predictZ * predictZ);
                        d = getDeviantity(pr, speed);
                        if (!(dx == predictX && dz == predictZ)) {
                            this.vlCount += (air == 1) ? 4 : 1;
                            this.vl += (d * 1.5) + (air == 1 ? 0.5 : (d > 0.04) ? 0.12 : 0.06);
                            if (this.vl > 0.05) {
                                this.ACCELERATION_MOTION /= 2;
                                flagged = true;
                            }
                            if (this.vl > 1 || (this.vl > 0.4 && this.vlCount > 10)) {
                                this.cancel("simulation[accel], diff: " + d);
                                this.vl -= 0.1;
                            }
                        }
                        //this.protocol.bukkit().sendMessage("vl: " + this.vl + " " + this.vlCount);
                    }
                }
                if (this.removePistonTick) {
                    this.protocol.getComponentXZ().pistonTick = false;
                    this.removePistonTick = false;
                }
                if (this.vl > 0) {
                    this.vl -= 0.002;
                    this.vl /= 1.1;
                }
                if (this.vlCount > 0) this.vlCount -= 0.3;
                if (this.vhact > 0) this.vhact--;
                this.lastDx = dx;
                this.lastDz = dz;
                if (!flagged) this.lastSpeed = speed;
                this.ACCELERATION_MOTION /= 1.14;
                if (this.nearSlimeStatus > 0) this.nearSlimeStatus--;
            }
        });
    }

    private double multiplex() {
        double m = 1.0;
        ExtendedPotionEffect speedEffect = this.protocol.spartan.getPotionEffect(PotionEffectType.SPEED, 5);
        Location l = this.protocol.getLocation().clone().add(0, -0.5, 0);
        Location lF = this.protocol.getFromLocation().clone().add(0, -0.5, 0);
        if (PlayerUtils.soulSpeed && this.protocol.bukkit().getInventory().getBoots() != null) {
            int e = this.protocol.bukkit().getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED);
            double v = (e + 1) * 0.4;
            m += v;
        }
        if (speedEffect != null)
            m += (double) (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.2;
        if (this.isOnIce(l) || this.isOnSlime(l) || this.isOnIce(lF) || this.isOnSlime(lF))
            m *= 1.6;
        if (this.protocol.spartan.getNearbyEntities(2).size() > 1)
            m *= 1.4;
        if (BlockUtils.areWebs(new SpartanLocation(this.protocol.getLocation()).getBlock().getType())) {
            m *= 0.2;
        }
        if (this.isOnWater() || this.isOnWater()) {
            ExtendedPotionEffect dolphinEffect = PlayerUtils.dolphinsGrace
                    ? this.protocol.spartan.getPotionEffect(PotionEffectType.DOLPHINS_GRACE, 0)
                    : null;
            if (dolphinEffect != null) m *= 6.2;
        }
        if (this.isOnSemi(l)) m *= 1.4;
        if (this.protocol.spartan.movement.isGliding()) m *= 18.0;
        if (this.protocol.spartan.movement.isFlying() || this.protocol.flyingTicks > 0) m *= 25.0;
        m *= (this.protocol.bukkit().getWalkSpeed() * 5);
        return m;
    }

    private static double getDeviantity(double a, double b) {
        return abs(abs(a) - abs(b));
    }

    private void velocity(double motion, double allowedDev, SpartanVector2D move) {
        PlayerVelocityEvent claimed = null;
        for (PlayerVelocityEvent velo : this.protocol.claimedVeloSpeed) {
            if (this.veloIsValid(motion, velo.getVelocity().getY())) {
                double speed = Math.sqrt(move.x * move.x + move.y * move.y);
                double pr = Math.sqrt(
                        velo.getVelocity().getX()
                                * velo.getVelocity().getX()
                                + velo.getVelocity().getZ()
                                * velo.getVelocity().getZ());
                if (speed < pr * 2 + allowedDev) {
                    this.predictX = move.x;
                    this.predictZ = move.y;
                    this.ACCELERATION_VELOCITY = (pr + allowedDev) * 1.6;
                }
                claimed = velo;
                this.velo = true;
                break;
            }
        }
        if (claimed != null) this.protocol.claimedVeloSpeed.remove(claimed);
    }

    private boolean veloIsValid(double motion, double velo) {
        return abs(abs(motion) - abs(velo)) < 0.08;
    }

    public void teleport() {
        this.call(() -> this.air = 0);
    }

    public void ground(boolean isOnGround) {
        this.call(() -> {
            this.air = isOnGround ? 0 : this.air + 1;
            this.action = this.protocol.useItemPacket ? this.action + 1 : 0;
            this.ground = isOnGround ? this.ground + 1 : 0;
        });
    }

    public void trident() {
        this.call(() -> this.tridentBoostApply = 10);
    }

    public void vhAct() {
        this.call(() -> this.vhact = 2);
    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private void setPredict(double x, double z) {
        this.predictX = x;
        this.predictZ = z;
    }

    private boolean isOnWater() {
        return this.protocol.getEncirclementData().isWater();
    }

    private boolean isOnIce(Location location) {
        return this.protocol.getEncirclementData().isIce();
    }

    private boolean isPointWall(Location... l) {
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = 0; dy <= 2; ++dy) {
                for (int dz = -2; dz <= 3; ++dz) {
                    for (Location location : l) {
                        double x = location.getX();
                        double y = location.getY();
                        double z = location.getZ();
                        Material material = this.protocol.packetWorld.getBlock(
                                new Location(
                                        this.protocol.getWorld(),
                                        x + (double) dx * 0.5,
                                        y + (double) dy * 0.5, z + (double) dz * 0.5));


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
        }
        return false;
    }


    private boolean isOnSemi(Location location) {
        return this.protocol.getEncirclementData().isSemi();
    }


    private boolean isOnSlime(Location location) {
        return this.protocol.getEncirclementData().isSlime();
    }

    private boolean isOnSlimeWide(Location location) {
        return this.protocol.getEncirclementData().isSlimeWide();
    }

}