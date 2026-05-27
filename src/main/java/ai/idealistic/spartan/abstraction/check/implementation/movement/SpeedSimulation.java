package ai.idealistic.spartan.abstraction.check.implementation.movement;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.event.CPlayerLungeEvent;
import ai.idealistic.spartan.abstraction.event.CPlayerRiptideEvent;
import ai.idealistic.spartan.abstraction.event.CPlayerVelocityEvent;
import ai.idealistic.spartan.abstraction.event.SuperPositionPacketEvent;
import ai.idealistic.spartan.abstraction.protocol.ExtendedPotionEffect;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.abstraction.world.ServerLocation;
import ai.idealistic.spartan.compatibility.manual.vanilla.Attributes;
import ai.idealistic.spartan.compatibility.necessary.protocollib.BackPlib;
import ai.idealistic.spartan.functionality.server.PluginBase;
import ai.idealistic.spartan.functionality.tracking.CheckConditions;
import ai.idealistic.spartan.listeners.protocol.TeleportListener;
import ai.idealistic.spartan.utils.math.MathHelper;
import ai.idealistic.spartan.utils.math.RayLine;
import ai.idealistic.spartan.utils.math.RayUtils;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import ai.idealistic.spartan.utils.minecraft.vector.CVector2D;
import ai.idealistic.spartan.utils.minecraft.world.BlockUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.potion.PotionEffectType;

public class SpeedSimulation extends CheckRunner {

    private final CheckDetection detection;

    private double vl = 0, vlCount = 0;

    private int ground = 0, air = 0, action = 0, vhact = 0, tpTicks = 0;
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

    public SpeedSimulation(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(
                this,
                null,
                null,
                null,
                true
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerTeleportEvent) {
            this.teleport();
        } else if (object instanceof SuperPositionPacketEvent) {
            SuperPositionPacketEvent e = (SuperPositionPacketEvent) object;
            this.ground(BackPlib.getSafeBoolean(e.packetEvent.getPacket(), 0));
        } else if (object instanceof CPlayerRiptideEvent
                || object instanceof CPlayerLungeEvent) {
            this.trident();
        } else if (object instanceof PlayerMoveEvent) {
            this.run();
        } else if (object instanceof VehicleEnterEvent) {
            this.vhAct();
        } else if (PluginBase.packetsEnabled()
                && object instanceof PacketEvent) {
            PacketType eventType = ((PacketEvent) object).getPacketType();

            for (PacketType type : TeleportListener.packetTypes) {
                if (type.equals(eventType)) {
                    this.teleport();
                    break;
                }
            }
        }
    }

    @Override
    protected boolean canRun() {
        return CheckConditions.canCheckMovement(
                this.protocol,
                true,
                true,
                false,
                true,
                false
        );
    }

    private boolean patches(PlayerProtocol p) {
        return p.wasGliding() || p.getVehicle() != null;
    }

    private void run() {
        this.detection.call(() -> {
            PlayerProtocol p = this.protocol;

            if (patches(p)) {
                this.ground = 0;
                this.air = 0;
                this.action = 0;
                this.vl = 0;
                this.vlCount = 0;
                this.velo = false;
                this.sprintStatus = true;
                this.ACCELERATION_MOTION = 0.0;
                this.ACCELERATION_VELOCITY = 0.0;
                this.predictX = 0;
                this.predictZ = 0;
                this.lastDx = 0;
                this.lastDz = 0;
                this.lastSpeed = 0;
                this.nearSlimeStatus = 0;
                this.slimeBoostApply = 0;
                this.tridentBoostApply = 0;
                this.removePistonTick = false;
                this.vhact = 0;
                this.tpTicks = 0;
                this.addInformationalDisableCause("Code-Patch");
                return;
            }
            boolean isOnGround = (p.getVehicle() != null)
                    ? p.getVehicle().isOnGround() : p.isOnGround();
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
            double atr = Math.max(
                    Attributes.getAmount(this.protocol, Attributes.GENERIC_MOVEMENT_SPEED),
                    0.0
            ) * 1.3;
            if (to.getX() != from.getX() || to.getY() != from.getY() || to.getZ() != from.getZ()) {
                {
                    this.ground(isOnGround);
                    if (this.air == 0) {
                        this.sprintStatus = false;
                        this.velo = false;

                        // Fix 2: Absorb sprint camera flicking by raising base limit from 0.235 to 0.285
                        double limit = isOnAngle ? 0.2975 : (isOnAngle80) ? 0.289 : 0.285;
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
                            lim = JUMP_SPRINT[Math.max(0, Math.min(air - 1, JUMP_SPRINT.length - 1))];
                        else if (RayUtils.validRayLines(yawLine, moveLine, 65)) {
                            lim = JUMP[Math.max(0, Math.min(air - 1, JUMP.length - 1))]
                                    + (JUMP_SPRINT[Math.max(0, Math.min(air - 1, JUMP_SPRINT.length - 1))] / 2.0);
                        } else lim = JUMP[Math.max(0, Math.min(air - 1, JUMP.length - 1))];
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
                        double speedAccel = Math.sqrt(getDeviance(dx, pX) * getDeviance(dx, pX) + getDeviance(dz, pZ) * getDeviance(dz, pZ));
                        double moveAngle = RayUtils.calculateRayLine(moveLine);
                        double accel = ((this.action > 2)
                                ? ACCELERATION_ACTION : (this.action > 1) ?
                                                        ACCELERATION_BASIC : ACCELERATION_SPRINT) * this.multiplex();
                        boolean nearbyEntities = false;
                        for (Entity entity : this.protocol.getNearbyEntities(2.4)) {
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
                            if (getDeviance(dx, pX) > accel) pX = dx;
                            if (getDeviance(dz, pZ) > accel) pZ = dz;
                        }
                        boolean isCorrect =
                                getDeviance(dx, pX) < accel
                                        && getDeviance(dz, pZ) < accel;
                        if (isCorrect)
                            this.setPredict(dx, dz);
                        else this.setPredict(
                                -Math.sin(Math.toRadians(to.getYaw())) * ACCELERATION_BASIC,
                                Math.cos(Math.toRadians(to.getYaw())) * ACCELERATION_BASIC);

                    }
                    this.velocity(
                            to.getY() - from.getY(),
                            ACCELERATION_SPRINT * (1.5) * this.multiplex(),
                            new CVector2D(dx, dz)
                    );
                    if ((this.isOnSlimeWide() || this.isOnSlimeWide()))
                        this.nearSlimeStatus = 4;
                    if (!(dx == predictX && dz == predictZ)) {
                        double pr = Math.sqrt(predictX * predictX + predictZ * predictZ);
                        double d = getDeviance(pr, speed);
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

                        // Fix 1: Expanded threshold for massive Lunge vectors from 3.1 to 8.5
                        if (d >= 0.01 && d <= 8.5 && this.tridentBoostApply > 0) {
                            this.tridentBoostApply--;
                            this.setPredict(dx, dz);
                        }

                        if (d >= 0.02 && d <= 1.7 && this.protocol.getComponentXZ().explosionTick) {
                            this.protocol.getComponentXZ().explosionTick = false;
                            this.setPredict(dx, dz);
                        }
                        if (this.protocol.getComponentXZ().pistonTick
                                && getDeviance(pr, speed) > 0.1 && getDeviance(pr, speed) <= 1.0) {
                            this.setPredict(dx, dz);
                            this.removePistonTick = true;
                        }
                        if (d >= 0.01 && d <= 4.3 && this.vhact > 0) {
                            this.setPredict(dx, dz);
                        }
                        if (this.isBlocksOnTop(to) && d < 0.1) {
                            this.setPredict(dx, dz);
                        }
                        if (this.protocol.predictedSlimeTicks > 0 && d < 2.75 && d > 0.01) {
                            this.setPredict(dx, dz);
                            this.protocol.predictedSlimeTicks--;
                        }
                        if (this.protocol.getVehicle() != null) {
                            this.setPredict(dx, dz);
                        }
                        pr = Math.sqrt(predictX * predictX + predictZ * predictZ);
                        d = getDeviance(pr, speed);

                        if (tpTicks > 0 && d > 1e-7 && d < 0.09) {
                            tpTicks--;
                            setPredict(dx, dz);
                            d = getDeviance(pr, speed);
                        }
                        if (!(dx == predictX && dz == predictZ)) {
                            this.vlCount += (air == 1) ? 4 : 1;
                            this.vl += (d * 1.5) + (air == 1 ? 0.5 : (d > 0.04) ? 0.12 : 0.06);
                            if (this.vl > 0.05) {
                                this.ACCELERATION_MOTION /= 2;
                                flagged = true;
                            }
                            if (this.vl > 1 || (this.vl > 0.4 && this.vlCount > 10)) {
                                double difference = Math.max(
                                        this.hackType.getCheck().getDecimalOption(
                                                "minimum_speed_difference",
                                                1e-10
                                        ),
                                        1e-10
                                );

                                if (this.getSpeedEffect() != null) {
                                    difference = Math.max(
                                            difference,
                                            0.13
                                    );
                                }
                                if (d >= difference) {
                                    this.detection.cancel(
                                            1.0 + d,
                                            "diff: " + d,
                                            this.protocol.getFromLocation(),
                                            0,
                                            true
                                    );
                                }
                                this.vl -= 0.1;
                            }
                        }
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

    private ExtendedPotionEffect getSpeedEffect() {
        return this.protocol.getPotionEffect(PotionEffectType.SPEED, 5);
    }

    private double multiplex() {
        double m = 1.0;
        ExtendedPotionEffect speedEffect = this.getSpeedEffect();
        boolean hasSpeed = speedEffect != null;
        Location l = this.protocol.getLocation().clone().add(0, -0.5, 0);
        Location lF = this.protocol.getFromLocation().clone().add(0, -0.5, 0);

        if (PlayerUtils.soulSpeed && this.protocol.getInventory().getBoots() != null) {
            int e = this.protocol.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED);
            double v = (e + 1) * 0.4;
            m += v;
        }

        if (speedEffect != null)
            m += (double) (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.2;

        if (this.isOnIce() || this.isOnSlime() || this.isOnIce() || this.isOnSlime())
            m *= 1.6;

        if (this.protocol.getNearbyEntities(2).size() > 1)
            m *= 1.4;

        if (BlockUtils.areWebs(new ServerLocation(this.protocol.getLocation()).getBlock(this.protocol).getType())) {
            m *= 0.2;
        }

        // Fix 3: Injected missing Depth Strider and Swim Animation multipliers
        if (this.isOnWater()) {
            if (this.protocol.getInventory().getBoots() != null) {
                int ds = this.protocol.getInventory().getBoots().getEnchantmentLevel(Enchantment.DEPTH_STRIDER);
                if (ds > 0) m += ds * 0.35;
            }
            if (this.protocol.bukkit().isSprinting()) {
                m *= 1.3;
            }
            ExtendedPotionEffect dolphinEffect = PlayerUtils.dolphinsGrace
                    ? this.protocol.getPotionEffect(PotionEffectType.DOLPHINS_GRACE, 0)
                    : null;
            if (dolphinEffect != null) m *= 6.2;
        }

        if (this.isOnSemi()) m *= 1.4;
        if (this.protocol.isGliding()) m *= 18.0;
        if (this.protocol.isFlying() || this.protocol.flyingTicks > 0) m *= 25.0;

        m *= (this.protocol.bukkit().getWalkSpeed() * 5);
        return m;
    }

    private static double getDeviance(double a, double b) {
        return abs(abs(a) - abs(b));
    }

    private void velocity(double motion, double allowedDev, CVector2D move) {
        CPlayerVelocityEvent claimed = null;

        for (CPlayerVelocityEvent velo : this.protocol.claimedVeloSpeed) {
            if (this.veloIsValid(motion, velo.getVelocity().getY())) {
                double speed = Math.sqrt(move.x * move.x + move.y * move.y);
                double pr = Math.sqrt(
                        velo.getVelocity().getX()
                                * velo.getVelocity().getX()
                                + velo.getVelocity().getZ()
                                * velo.getVelocity().getZ());
                if (speed < pr * 2.5 + allowedDev) {
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

    private void teleport() {
        this.detection.call(() -> {
            this.tpTicks = 1;
            this.air = 0;
        });
    }

    private void ground(boolean isOnGround) {
        this.detection.call(() -> {
            this.air = isOnGround ? 0 : this.air + 1;
            this.action = this.protocol.useItemPacket ? this.action + 1 : 0;
            this.ground = isOnGround ? this.ground + 1 : 0;
        });
    }

    // Fix 1 Continued: Extending active threshold ticks for momentum decay
    private void trident() {
        this.detection.call(() -> this.tridentBoostApply = 20);
    }

    private void vhAct() {
        this.detection.call(() -> this.vhact = 2);
    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private void setPredict(double x, double z) {
        this.predictX = x;
        this.predictZ = z;
    }

    private boolean isOnWater() {
        return this.protocol.getEnvironment().isLiquid();
    }

    private boolean isOnIce() {
        return this.protocol.getEnvironment().isIce();
    }

    private boolean isPointWall(Location... l) {
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dy = 0; dy <= 2; ++dy) {
                for (int dz = -2; dz <= 3; ++dz) {
                    for (Location location : l) {
                        double x = location.getX();
                        double y = location.getY();
                        double z = location.getZ();
                        Material material = new ServerLocation(
                                location.getWorld(),
                                x + (double) dx * 0.5,
                                y + (double) dy * 0.5, z + (double) dz * 0.5
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
                    Material material = new ServerLocation(location.getWorld(), x + (double) dx * 0.5, y + dy, z + (double) dz * 0.5).getBlock(
                            protocol
                    ).getTypeOrNull();

                    if (material != null && BlockUtils.isSolid(material)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isOnSemi() {
        return this.protocol.getEnvironment().isSemi();
    }

    private boolean isOnSlime() {
        return this.protocol.getEnvironment().isSlime();
    }

    private boolean isOnSlimeWide() {
        return this.protocol.getEnvironment().isSlimeWide();
    }

}