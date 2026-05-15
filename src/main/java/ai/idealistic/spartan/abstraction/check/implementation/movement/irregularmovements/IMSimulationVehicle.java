package ai.idealistic.spartan.abstraction.check.implementation.movement.irregularmovements;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.data.EnvironmentData;
import ai.idealistic.spartan.abstraction.protocol.ExtendedPotionEffect;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.utils.minecraft.entity.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.potion.PotionEffectType;

public class IMSimulationVehicle extends CheckDetection {

    private double vl = 0, oldY = 0, oldSpeed = 0;
    private int ground = 0, air = 0, ticks = 0, postFly = 0;
    private Location from = null;

    public IMSimulationVehicle(CheckRunner executor) {
        super(
                executor,
                null,
                Check.DetectionType.PACKETS,
                "vehicle",
                true
        );
    }

    void run() {
        this.call(() -> {
            PlayerProtocol p = this.protocol;
            Entity vehicleEntity = p.getVehicle();

            if (vehicleEntity == null) {
                this.ticks = 0;
                this.vl = 0;
                this.postFly = 0;
                this.air = 0;
                this.ground = 0;
                this.from = null;
                this.oldY = 0;
                this.oldSpeed = 0;
                return;
            }

            // Note: In older Bukkit versions, living entities might not inherit Vehicle.
            // Using Entity directly prevents ClassCastExceptions while maintaining safety.
            this.ticks++;
            boolean isOnGround = vehicleEntity.isOnGround();
            this.ground(isOnGround);
            Location to = vehicleEntity.getLocation();
            if (from == null) from = to.clone();

            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            double y = to.getY() - from.getY();

            final EnvironmentData ec = protocol.getEnvironment();
            double prV = (vehicleEntity instanceof Boat) ? 0.04 : 0.08 * 0.98f;
            double pr = Math.abs(Math.abs(y) - Math.abs(oldY - prV));

            boolean isLaunched = ec.isBubble() || ec.isSlime() || ec.isSlimeWide() || ec.isSlimeHeight();

            // Detection 1: Artificial Vehicle Flight (Y-Axis Constant Velocity)
            if (pr > 1e-4 && air > 4 && (vehicleEntity instanceof Boat)) {
                if (!isLaunched) {
                    if (postFly < 0) postFly = 0;
                    this.postFly++;
                    if (this.postFly > 10) {
                        this.cancel("prediction vehicle gravity: " + pr, this.protocol.getFromLocation(), 0, true);
                        this.postFly = 9;
                    }
                }
            } else if (!(vehicleEntity instanceof Minecart) && getDeviance(oldY, prV) < 1e-10) {
                // Enforce air > 4 requirement here as well to prevent false positives when falling off blocks
                if (!isLaunched && air > 4) {
                    this.postFly++;
                    if (this.postFly > 7) {
                        this.cancel("prediction vehicle gravity: " + pr, this.protocol.getFromLocation(), 0, true);
                        this.postFly = 6;
                    }
                }
            } else if (this.postFly > 0) {
                this.postFly -= 2;
            }

            // EXTENSION: The 'diffAccel' (prediction vehicle speed) check was completely removed here.
            // It applied player-friction math (0.91) to vehicles, causing false positives on horses/pigs.

            if (!ec.isAllFalse()) this.postFly -= 2;

            // Detection 2: Hard Speed & Vertical Limits
            if (ticks > 1) {
                double allowedSpeed = 1.0;
                double allowedVertical = 1.0;

                // Handle Living Entities specifically
                if (vehicleEntity instanceof LivingEntity) {
                    if (air == 0) allowedSpeed = 1.05;
                    else if (air < 4) allowedSpeed = 1.7;
                    else if (air < 14) allowedSpeed = 1.2;
                    else allowedSpeed = 0.4;

                    if (air < 2) allowedVertical = 1.5;
                    else if (air < 4) allowedVertical = 1.2 + (oldY - (0.98f * 0.08) + 0.03);
                    else if (air < 18) allowedVertical = oldY - (0.98f * 0.08) + 0.6;
                    else allowedVertical = oldY - (0.98f * 0.08);
                }

                allowedSpeed *= this.multiplex(vehicleEntity);

                if (ec.isSemi()) {
                    allowedSpeed += 1.0;
                    allowedVertical += 0.75;
                }
                if (ec.isGlide() || ec.isLiquid()) {
                    allowedSpeed += 0.2;
                    if (allowedVertical < 0) allowedVertical = 0;
                    allowedVertical += 0.3;
                }
                if (ec.isSlimeHeight() || ec.isBubble()) {
                    allowedSpeed += 1.5;
                    allowedVertical += 1.5;
                }

                if (speed > allowedSpeed) {
                    this.vl += speed / allowedSpeed;
                    if (this.vl > 12.0) {
                        this.cancel("vehicle limit speed: " + speed, this.protocol.getFromLocation());
                        this.vl /= 1.3;
                    }
                }

                if (y > allowedVertical) {
                    this.vl += Math.abs(y / allowedVertical);
                    if (this.vl > 15.0) {
                        this.cancel("vehicle limit gravity: " + y, this.protocol.getFromLocation());
                        this.vl /= 1.3;
                    }
                }
                this.vl /= 1.2;
            }
            from = to.clone();
            oldY = y;
            oldSpeed = speed;
        });
    }

    private double multiplex(Entity vehicle) {
        if (vehicle instanceof Boat) {
            if (this.isOnIce()) return 4.5;
            if (this.isOnWater()) return 1.5;
            return 0.8;
        } else if (vehicle instanceof Minecart) {
            return 1.2;
        }

        double m = 1.0;
        ExtendedPotionEffect speedEffect = this.protocol.getPotionEffect(PotionEffectType.SPEED, 5);

        if (PlayerUtils.soulSpeed && this.protocol.getInventory().getBoots() != null) {
            int e = this.protocol.getInventory().getBoots().getEnchantmentLevel(Enchantment.SOUL_SPEED);
            double v = (e + 1) * 0.4;
            m += v;
        }
        if (speedEffect != null) {
            m += (double) (speedEffect.bukkitEffect.getAmplifier() + 1) * 0.2;
        }
        if (this.isOnIce() || this.isOnSlime()) {
            m *= 1.6;
        }
        if (this.isOnWater()) {
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

    void teleport() {
        this.call(() -> {
            this.air = 0;
            this.ticks = 0;
        });
    }

    private void ground(boolean isOnGround) {
        this.call(() -> {
            this.air = isOnGround ? 0 : this.air + 1;
            this.ground = isOnGround ? this.ground + 1 : 0;
        });
    }

    private static double abs(double v) {
        return Math.abs(v);
    }

    private boolean isOnWater() {
        return this.protocol.getEnvironment().isLiquid();
    }

    private boolean isOnIce() {
        return this.protocol.getEnvironment().isIce();
    }

    private boolean isOnSemi() {
        return this.protocol.getEnvironment().isSemi();
    }

    private boolean isOnSlime() {
        return this.protocol.getEnvironment().isSlime();
    }
}