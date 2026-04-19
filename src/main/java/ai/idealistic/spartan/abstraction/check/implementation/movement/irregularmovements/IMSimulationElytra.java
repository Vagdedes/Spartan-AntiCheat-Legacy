package ai.idealistic.spartan.abstraction.check.implementation.movement.irregularmovements;

import ai.idealistic.spartan.abstraction.check.Check;
import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.data.Buffer;
import ai.idealistic.spartan.abstraction.data.EnvironmentData;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.utils.math.RayUtils;
import kireiko.dev.millennium.math.Statistics;
import kireiko.dev.millennium.types.EvictingList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.List;

public class IMSimulationElytra extends CheckDetection {

    private double vl = 0, oldY = 0, oldSpeed = 0, oldAccel = 0;
    private int ground = 0, air = 0, ticks = 0, postSpeed = 0;
    private final List<Double> lastYMotions = new EvictingList<>(25);
    private Location from = null;
    private final Buffer.IndividualBuffer buffer;

    private Vector lastVelocity = new Vector(0, 0, 0);
    private boolean wasGliding = false;
    private int glidingTicks = 0;
    private float newVl = 0;
    private int fireworkBoostTicks = 0;
    private double lastHorizontalSpeedSq = 0;

    private static final double DRAG = 0.991D;
    private static final double DRAG_HORIZONTAL_SQ = DRAG * DRAG * 1.001;
    private static final double VERTICAL_DRAG_SLOW = 0.999D;
    private static final double GRAVITY = 0.0485D;
    private static final double LIFT_CONSTANT = 0.035D;
    private final float bufferThreshold = 4.0f;
    private final double baseThreshold = 0.023d;
    private final double rotationLeniencyFactor = 0.0008d;
    private final double divingLeniencyFactor = 0.0005d;


    public IMSimulationElytra(CheckRunner executor) {
        super(
                executor,
                null,
                Check.DetectionType.PACKETS,
                "elytra_experimental",
                false
        );
        this.buffer = new Buffer.IndividualBuffer();
    }

    public void handleVelocity() {
        newVl = Math.max(-2, newVl - 6.0f);
    }

    public void handleInteract() {
        if (protocol.bukkit().isGliding()) {
            PlayerInventory inv = protocol.bukkit().getInventory();
            ItemStack mainHand = inv.getItemInMainHand();
            ItemStack offHand = inv.getItemInOffHand();

            // EXTENSION: Patched Offhand Firework and Trident Riptide blindspots
            boolean hasBoostItem = false;
            if (mainHand != null && (mainHand.getType().name().contains("FIREWORK") || mainHand.getType().name().contains("TRIDENT"))) {
                hasBoostItem = true;
            } else if (offHand != null && (offHand.getType().name().contains("FIREWORK") || offHand.getType().name().contains("TRIDENT"))) {
                hasBoostItem = true;
            }

            if (hasBoostItem) {
                this.fireworkBoostTicks = 75;
                this.newVl = 0;
            }
        }
    }

    void runExploit() {
        this.call(() -> {
            if (protocol.getVehicle() == null) {
                PlayerInventory inventory = protocol.getInventory();
                ItemStack i = inventory.getChestplate();

                if (i != null) {
                    if (i.getType() == Material.ELYTRA) {
                        if (i.getDurability() < 432) {
                            double buffer = this.buffer.count(1, 20),
                                    threshold = 15;

                            if (buffer >= threshold) {
                                cancel(
                                        "elytra(glide-spam)",
                                        this.protocol.getFromLocation(),
                                        0,
                                        true
                                );

                                if (this.prevent()) {
                                    protocol.bukkit().setGliding(false);
                                }
                            }
                        } else {
                            cancel(
                                    "elytra(no-durability-glide)",
                                    this.protocol.getFromLocation(),
                                    0,
                                    true
                            );

                            if (this.prevent()) {
                                protocol.bukkit().setGliding(false);
                            }
                        }
                    } else {
                        cancel(
                                "elytra(no-elytra-glide)",
                                this.protocol.getFromLocation(),
                                0,
                                true
                        );

                        if (this.prevent()) {
                            protocol.bukkit().setGliding(false);
                        }
                    }
                } else {
                    cancel(
                            "elytra(no-chestplate-item-glide)",
                            this.protocol.getFromLocation(),
                            0,
                            true
                    );

                    if (this.prevent()) {
                        protocol.bukkit().setGliding(false);
                    }
                }
            } else {
                cancel(
                        "elytra(vehicle-glide)",
                        this.protocol.getFromLocation(),
                        0,
                        true
                );

                if (this.prevent()) {
                    protocol.bukkit().setGliding(false);
                }
            }
        });
    }

    void runSimulation() {
        this.call(() -> {
            PlayerProtocol p = this.protocol;

            if (!p.isGliding()) {
                this.ticks = 0;
                this.air = 0;
                this.ground = 0;
                this.vl = 0;
                this.postSpeed = 0;
                this.from = null;
                this.oldY = 0;
                this.oldSpeed = 0;
                this.oldAccel = 0;

                if (wasGliding) {
                    this.wasGliding = false;
                    this.glidingTicks = 0;
                    this.newVl = 0;
                    this.lastVelocity = new Vector(0, 0, 0);
                    this.fireworkBoostTicks = 0;
                    this.lastHorizontalSpeedSq = 0;
                }
                return;
            }

            this.ticks++;
            boolean isOnGround = p.isOnGround();
            this.ground(isOnGround);
            Location to = p.getLocation();
            if (from == null) from = to.clone();
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double speed = Math.sqrt(dx * dx + dz * dz);
            double accel = getDeviance(speed, (oldSpeed * 0.91));
            double diffAccel = getDeviance(accel, oldAccel);
            double y = to.getY() - from.getY();
            final EnvironmentData ec = protocol.getEnvironment();
            double pr = Math.abs(Math.abs(y) - Math.abs(oldY));
            if (fireworkBoostTicks > 0) {
                this.postSpeed = 0;
                this.vl = 0;
            }
            if (ticks > 1) {
                if (pr != 0) lastYMotions.add(pr);
                final boolean isInvalid = lastYMotions.isEmpty() || (Statistics.getMax(lastYMotions) > 1e-7);
                if (isInvalid) {
                    if (speed > 0.05 && ec.isAllFalse() && diffAccel < 1e-8) {
                        this.postSpeed += (diffAccel == 0) ? 3 : 1;
                        if (this.postSpeed > 12) {
                            this.cancel("invalid elytra speed: " + diffAccel, this.protocol.getFromLocation(), 0, true);
                            this.postSpeed = 8;
                        }
                    } else if (this.postSpeed > 0) this.postSpeed -= 2;
                    if (pr == 0.0 && ec.isAllFalse()) {
                        this.vl += 2.0;
                        if (this.vl >= 5.0) {
                            this.cancel("invalid elytra gravity: " + pr, this.protocol.getFromLocation(), 0, true);
                            this.vl /= 1.3;
                        }
                    }
                    if (speed > 2.7 || y > 2.0) {
                        this.cancel("elytra limit", this.protocol.getFromLocation(), 0, true);
                    }
                }
                this.vl /= 1.15;
            }

            handleNewMoveChecks(from, to);

            from = to.clone();
            oldY = y;
            oldSpeed = speed;
            oldAccel = accel;
        });
    }

    private void handleNewMoveChecks(Location from, Location to) {
        if (fireworkBoostTicks > 0) {
            fireworkBoostTicks--;
            Vector currentVelocity = new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
            currentVelocity.multiply(-1);
            this.lastVelocity = currentVelocity;
            this.lastHorizontalSpeedSq = Math.pow(currentVelocity.getX(), 2) + Math.pow(currentVelocity.getZ(), 2);
            return;
        }

        wasGliding = true;
        glidingTicks++;

        Vector actualVelocity = new Vector(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());

        // EXTENSION: Fixed Quick-Deploy Jump false positive by checking acceleration, not raw Y velocity
        if (glidingTicks < 6 && (actualVelocity.getY() > lastVelocity.getY() + 0.05) && fireworkBoostTicks < 1) {
            cancel("Elytra (Limit) gain: " + RayUtils.scaleVal(actualVelocity.getY() - lastVelocity.getY(), 4), from, 0, true);
            lastVelocity = new Vector(0, 0, 0);
            if (prevent()) {
                protocol.bukkit().setGliding(false);
            }
            glidingTicks = 0;
            return;
        }

        if (glidingTicks < 2) {
            lastVelocity = actualVelocity;
            lastHorizontalSpeedSq = Math.pow(actualVelocity.getX(), 2) + Math.pow(actualVelocity.getZ(), 2);
            return;
        }

        double actualHorizontalSpeedSq = Math.pow(actualVelocity.getX(), 2) + Math.pow(actualVelocity.getZ(), 2);
        boolean isFlyingLevel = Math.abs(actualVelocity.getY()) < 0.009;

        double expectedDragSq = lastHorizontalSpeedSq * DRAG_HORIZONTAL_SQ;

        // EXTENSION: Fixed the floating-point strict-equality bypass
        if (isFlyingLevel && actualHorizontalSpeedSq > 0.02 && actualHorizontalSpeedSq >= (expectedDragSq - 0.0001)) {
            if (++newVl > bufferThreshold) {
                cancel("Elytra (Energy) artificial horizontal maintenance: " + actualHorizontalSpeedSq, from, 0, true);
            }
        } else {
            Vector predictedVelocity = lastVelocity.clone();
            Vector lookVector = getLookDirection(from.getYaw(), from.getPitch());
            predictedVelocity.add(lookVector.clone().multiply(LIFT_CONSTANT));
            predictedVelocity.setY(predictedVelocity.getY() - GRAVITY);
            double dragMultiplier = Math.abs(predictedVelocity.getY()) < 0.2 ? VERTICAL_DRAG_SLOW : DRAG;
            predictedVelocity.multiply(dragMultiplier);

            double delta = predictedVelocity.distanceSquared(actualVelocity);

            float pitch = to.getPitch();
            double yawDelta = getAngleDifference(from.getYaw(), to.getYaw());
            double pitchDelta = Math.abs(from.getPitch() - pitch);
            double totalRotation = yawDelta + pitchDelta;

            double diveBonus = 0.0D;
            if (pitch > 30.0F) {
                diveBonus = (pitch - 30.0F) * divingLeniencyFactor;
            }
            double dynamicThreshold = baseThreshold + (totalRotation * rotationLeniencyFactor) + diveBonus;

            if (delta > dynamicThreshold) {
                if (++newVl > bufferThreshold) {
                    cancel("Elytra (Prediction) d: " + delta + ", pr: " + dynamicThreshold, from, 0, true);
                }
            } else {
                newVl = Math.max(0, newVl - 0.15f);
            }
        }

        this.lastVelocity = actualVelocity;
        this.lastHorizontalSpeedSq = actualHorizontalSpeedSq;
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

    private Vector getLookDirection(float yaw, float pitch) {
        Vector vector = new Vector();
        double pitchRad = Math.toRadians(pitch);
        double yawRad = Math.toRadians(yaw);
        vector.setY(-Math.sin(pitchRad));
        double xz = Math.cos(pitchRad);
        vector.setX(-xz * Math.sin(yawRad));
        vector.setZ(xz * Math.cos(yawRad));
        return vector.normalize();
    }

    private double getAngleDifference(double a, double b) {
        double diff = Math.abs(a - b) % 360;
        return diff > 180 ? 360 - diff : diff;
    }
}