package com.vagdedes.spartan.abstraction.check.implementation.movement.speed;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.compatibility.manual.vanilla.Attributes;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.RayLine;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import me.vagdedes.spartan.system.Enums;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Speed extends CheckExecutor {

    /*
    Anticipate movements in advance = have good performance in the future
     */
    public static final double[] BASIC_SPEED = new double[]{
            0.63, // 0
            0.37,
            0.37,
            0.36,
            0.35,
            0.35,
            0.34,
            0.34,
            0.34,
            0.33,
            0.32,
            0.32,
            0.32,
            0.31,
            0.31,
            0.31,
            0.3 // 16
    };

    public static final double[] FLY_SPEED = new double[]{
            0.42, 0.34, 0.25, 0.17, 0.09, 0.01, -0.07, -0.15, -0.22, -0.3,
            -0.37, -0.44, -0.51, -0.58, -0.64, -0.71, -0.77, -0.84, -0.9,
            -0.96, -1.02, -1.08, -1.13, -1.24, -1.19, -1.3, -1.35, -1.4,
            -1.45, -1.5, -1.55, -1.6, -1.64, -1.69, -1.73, -1.78, -1.82,
            -1.86, -1.9, -1.94, -1.98, -2.02, -2.06, -2.1, -2.13, -2.17,
            -2.2, -2.24, -2.27, -2.3, -2.34, -2.37, -2.4, -2.43, -2.46,
            -2.49, -2.52, -2.54, -2.6, -2.57, -2.65, -2.62, -2.68, -2.7,
            -2.72, -2.75, -2.77, -2.79, -2.82, -2.84, -2.86, -2.88, -2.9,
            -2.92, -2.94, -2.96, -2.98, -3.0
    };

    private int onAir = 0, onGround = 0, velocity = 0;
    private double multiply = 0, multiplyFriction = 0, multiplyVelo = 0, vl = 0, multiplyViolation;
    private double yaw1move = 0;
    private String violation = null;
    private RayLine tick1move = new RayLine(0, 0);
    private Location f = null;
    private boolean simulationFlag = false;

    public Speed(Enums.HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, true);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object instanceof PlayerMoveEvent) {
            moveTick((PlayerMoveEvent) object);
        } else if (object instanceof PlayerVelocityEvent) {
            PlayerVelocityEvent e = (PlayerVelocityEvent) object;
            Vector v = e.getVelocity();
            double dx = v.getX();
            double dz = v.getZ();
            multiplyVelo = Math.sqrt(dx * dx + dz * dz) * 2;
            velocity = 2;
        } else if (object.toString().equals("TRIDENT")) {
            multiply = 4.0D;
        } else if (object.toString().equals("RIPTIDE")) {
            multiply = 10.0D;
        }
    }

    private void moveTick(PlayerMoveEvent e) {
        Location location = this.player.protocol.getLocation().clone();
        if (f == null) f = location.clone();
        Location from = e.getFrom();
        Location to = e.getTo();
        boolean onGround = this.player.protocol.isOnGround();
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();

        double multiply = updateStats();
        double speed = r(Math.sqrt(dx * dx + dz * dz));
        double speedLimit = r(((onGround || onAir > 17)
                ? BASIC_SPEED[16] : BASIC_SPEED[onAir - 1]) * multiply);
        double radiYaw = Math.toRadians(location.getYaw());
        RayLine lookRay = new RayLine(-Math.sin(radiYaw), Math.cos(radiYaw));
        RayLine moveRay = new RayLine(dx, dz);
        if (onAir == 1 || onGround) {
            tick1move = moveRay;
            yaw1move = radiYaw;
        }
        double rayDiff = RayUtils.calculateRayLines(lookRay, moveRay);
        SpartanPotionEffect glideEffect = PlayerUtils.slowFall
                ? player.getPotionEffect(PotionEffectType.SLOW_FALLING, 0)
                : null;
        SpartanPotionEffect levitationEffect = PlayerUtils.levitation
                ? player.getPotionEffect(PotionEffectType.LEVITATION, 0)
                : null;

        if (speed > speedLimit) {
            this.multiplyViolation = speed / speedLimit;
            this.vl += 35 * multiplyViolation;

            if (hackType.getCheck().getBooleanOption("straight_line_speeding", true)) {
                this.violation = "type: straight line speeding [" + speed + "/" + speedLimit + "], onGround: " + onGround;
            } else {
                this.violation = null;
            }
        }
        if (velocity == 0 && !player.bedrockPlayer && levitationEffect == null
                && glideEffect == null && !player.movement.isSwimming()) {
            double localSpeedLimit = r((speedLimit / 1.35) + 0.01);
            double localSpeedLimit2 = r((speedLimit / 1.6) + 0.01);
            double[] allowedRots = new double[]{0.0, 45.0, -45.0};
            double airDiff = RayUtils.calculateRayLines(tick1move, moveRay);
            byte correction = 0;
            for (double m : allowedRots) {
                double radiYawLocal = Math.toRadians(location.getYaw() + m);
                RayLine lookRayLocal = new RayLine(-Math.sin(radiYawLocal), Math.cos(radiYawLocal));
                double diff = RayUtils.calculateRayLines(lookRayLocal, tick1move);
                if (diff < 25) {
                    correction++;
                }
            }
            if (((correction == 0 || rayDiff > 88) && onGround && speed > localSpeedLimit)
                    || ((airDiff > 45) && !onGround && speed > localSpeedLimit)) {
                this.multiplyViolation = speed / localSpeedLimit;
                this.vl += 25 * multiplyViolation;

                if (hackType.getCheck().getBooleanOption("strafe_line_speeding", true)) {
                    this.violation = "type: strafe line speeding [" +
                            speed + "/" + localSpeedLimit + "], onGround: " + onGround + ", diff: " + rayDiff;
                } else {
                    this.violation = null;
                }
            }
            if (onAir == 1 && rayDiff > 47 && speed > localSpeedLimit2) {
                this.multiplyViolation = speed / localSpeedLimit2;
                this.vl += 75 * multiplyViolation;

                if (hackType.getCheck().getBooleanOption("strafe_jump_speeding", true)) {
                    this.violation = "type: strafe jump speeding [" +
                            speed + "/" + localSpeedLimit2 + "], onGround: " + onGround + ", diff: " + rayDiff;
                } else {
                    this.violation = null;
                }
            }
        }


        if (this.vl > 125) {
            this.vl /= 2;

            if (this.violation != null) {
                cancel(this.violation, this.multiplyViolation);
            }
            startSimulationFlag();
        }


        this.vl /= 1.07;
        if (onGround) {
            this.multiplyVelo /= 1.4;
            this.multiply /= 1.3;
            this.multiplyFriction /= 1.45;
            if (this.velocity > 0) velocity--;
        }
        //player.getInstance().sendMessage("i: " + speed + " " + speedLimit + " " + rayDiff + " " + allowedAngle(rayDiff));
    }

    private double updateStats() {
        if (!this.player.protocol.isOnGround()) {
            onAir++;
            onGround = 0;
        } else {
            onGround++;
            onAir = 0;
        }
        SpartanLocation to = new SpartanLocation(this.player.protocol.getLocation().clone());
        PlayerInventory inventory = player.getInstance().getInventory();
        Entity vehicle = this.player.getInstance().getVehicle();
        if (onGround == 1) this.multiplyFriction = 0.26;
        double m = (this.player.getInstance().getWalkSpeed() * 5);
        boolean solidBlockOnTop = RayUtils.onSolidBlock(player, to.clone().add(0, 2, 0));
        m += getMaxSpeedAmplifier();
        SpartanPotionEffect dolphinEffect = PlayerUtils.dolphinsGrace
                ? this.player.getPotionEffect(PotionEffectType.DOLPHINS_GRACE, 0)
                : null;
        SpartanPotionEffect speedEffect = this.player.getPotionEffect(PotionEffectType.SPEED, 0);
        SpartanPotionEffect glideEffect = PlayerUtils.slowFall
                ? player.getPotionEffect(PotionEffectType.SLOW_FALLING, 0)
                : null;
        SpartanPotionEffect levitationEffect = PlayerUtils.levitation
                ? player.getPotionEffect(PotionEffectType.LEVITATION, 0)
                : null;
        if (PlayerUtils.soulSpeed && inventory.getBoots() != null) {
            int e = inventory.getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED);
            double v = (e + 1) * 0.4;
            if (e > 0) {
                if (this.multiply < v) this.multiply = v;
            }
        }
        if (dolphinEffect != null) if (this.multiply < 5.0D) this.multiply = 5.0D;
        if (slimeBlockCause(this.player, new SpartanLocation(this.player.protocol.getLocation()))) {
            if (this.multiply < 3) this.multiply = 3D;
        }
        if ((this.onIce(player, this.player.movement.getLocation().clone()))) {
            if (this.multiply < 0.7) multiply = 0.7;
        }
        if (solidBlockOnTop) {
            if (this.multiply < 0.4) this.multiply = 0.4D;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.honey_block)) {
            if (multiply < 0.2) multiply = 0.2;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.climbableOriginal)) {
            if (multiply < 0.3) multiply = 0.3;
        }
        if (RayUtils.onBlock(player, to, BlockUtils.stairs)) {
            if (multiply < 0.35) multiply = 0.35;
        }
        if (this.player.movement.isGliding() || vehicle != null) {
            if (multiply < 10.0) multiply = 10.0;
        }
        if (this.player.movement.isFlying()) {
            multiply = 25.0;
        }
        for (Entity e : player.getNearbyEntities(2, 2, 2)) {
            if (multiply < 0.4 && !(e instanceof Player)) {
                multiply = 0.4;
                break;
            }
        }

        m *= 1 + (((speedEffect != null ? (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.4 : 0))
                + (this.onIce(player, this.player.movement.getLocation().clone()) ? 0.66 : 0)
                + multiply + multiplyVelo
                + ((this.player.protocol.isOnGround()) ? multiplyFriction : 0));
        if (levitationEffect != null || glideEffect != null) m *= 1.3;
        SpartanBlock block = new SpartanLocation(this.player.protocol.getLocation()).getBlock();
        if (BlockUtils.areWebs(block.material)) {
            m /= 4.0;
        }

        return m;
    }


    private boolean slimeBlockCause(SpartanPlayer player, SpartanLocation location) {
        boolean slime = false;
        World world = player.getWorld();

        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y2 = location.getBlockY() - 1; y2 <= location.getBlockY() + 1; y2++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    SpartanBlock block = new SpartanLocation(world, x, y2, z, 0.0f, 0.0f).getBlock();
                    if (BlockUtils.areSlimeBlocks(block.material)) {
                        slime = true;
                    }
                }
            }
        }
        return slime;
    }

    private boolean onIce(SpartanPlayer player, SpartanLocation location) {
        World world = player.getWorld();
        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y2 = location.getBlockY() - 2; y2 <= location.getBlockY() + 1; y2++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    SpartanBlock block = new SpartanLocation(world, x, y2, z, 0.0f, 0.0f).getBlock();
                    if (BlockUtils.areSlimeBlocks(block.material) || BlockUtils.areIceBlocks(block.material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double getMaxSpeedAmplifier() {
        return Attributes.getAmount(this.player, Attributes.GENERIC_MOVEMENT_SPEED);
    }

    private void startSimulationFlag() {
        this.f = this.player.movement.getEventFromLocation().getBukkitLocation();
        this.simulationFlag = true;
    }

    private boolean allowedAngle(double angle) {
        double a = angle / 45;
        int wrapGoodMath = (int) Math.floor(a);
        double b = a - wrapGoodMath;
        return (b < 0.3 || b > 0.7);
    }

    private double r(double v) {
        return RayUtils.scaleVal(v, 2);
    }

    @Override
    protected boolean canRun() {
        // Check will no longer run when someone uses server flying
        // Do not change as there is a protection for it via the IrregularMovements check
        // The flying method should still be considered in the code, in case anything ever changes
        return MovementProcessing.canCheck(player, true, true, true, true);
    }

}
