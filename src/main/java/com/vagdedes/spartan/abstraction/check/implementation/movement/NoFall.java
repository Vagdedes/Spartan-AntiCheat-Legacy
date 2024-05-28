package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class NoFall extends CheckExecutor {

    private static final double
            drag = 0.98,
            acceleration = PlayerUtils.airAcceleration * drag,
            terminalVelocity = PlayerUtils.calculateTerminalVelocity(
                    PlayerUtils.airDrag,
                    PlayerUtils.airAcceleration
            );
    private static final float significantFallDistance = 3.0f;
    private static final Map<Double, Integer> fallTicksLibrary = new LinkedHashMap<>();

    static {
        double previous = 0.0;

        for (int fallTick = 0; fallTick < 500; fallTick++) {
            double prediction = fallTick == 0 ? 0.0 :
                    fallTick == 1 ? acceleration
                            : (previous * drag) + (fallTick * acceleration);
            previous = prediction;
            fallTicksLibrary.put(AlgebraUtils.cut(prediction, MovementProcessing.heightPrecision), fallTick);
        }
    }

    private static final class ViolationObject {

        private final SpartanPlayer player;
        private final String info;
        private final long tick;
        private final double violations, damage;

        private ViolationObject(SpartanPlayer player, String info, double violations, double damage) {
            this.tick = TPS.getTick(player);
            this.player = player;
            this.info = info;
            this.violations = violations;
            this.damage = damage;
        }

        private boolean valid() {
            return TPS.getTick(player) - this.tick <= 2;
        }
    }

    private double previous, distance;
    private final List<ViolationObject> violationObjects;

    public NoFall(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
        this.violationObjects = Collections.synchronizedList(new LinkedList<>());
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        if (object == null) {
            this.previous = player.getFallDistance();
            this.distance = 0.0;
        } else if (object == EntityDamageEvent.DamageCause.FALL
                && !this.violationObjects.isEmpty()) {
            synchronized (this.violationObjects) {
                Iterator<ViolationObject> iterator = this.violationObjects.iterator();

                while (iterator.hasNext()) {
                    ViolationObject violationObject = iterator.next();

                    if (!violationObject.valid()) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void cannotHandle(boolean cancelled, Object object) {
        this.handleInternal(cancelled, object);
    }

    @Override
    public void runInternal(boolean cancelled) {
        Double vertical = player.movement.getEventVertical();
        double fallDistance = player.getFallDistance();

        if (vertical == null) {
            this.previous = fallDistance;
            this.distance = 0.0;
            return;
        }
        int fallDistanceTick = getFallTicks(previous) + 1;
        boolean toGround = player.isOnGround() || player.isOnGround(player.movement.getEventToLocation(), true),
                fromGround = player.isOnGround(player.movement.getEventFromLocation(), true);

        if (!this.violationObjects.isEmpty()) {
            Iterator<ViolationObject> iterator = this.violationObjects.iterator();

            while (iterator.hasNext()) {
                ViolationObject violationObject = iterator.next();

                if (violationObject.valid()) {
                    iterator.remove();
                    cancel(
                            violationObject.info,
                            violationObject.violations,
                            null,
                            0,
                            false,
                            violationObject.damage
                    );
                }
            }
        }
        runAsync(() -> {
            double prediction = fallDistanceTick == 1
                    ? acceleration
                    : (this.previous * drag) + (fallDistanceTick * acceleration),
                    difference = Math.abs(fallDistance - prediction),
                    limit = vertical >= 0.0
                            ? PlayerUtils.airAcceleration
                            : ((0.0 - vertical) / terminalVelocity) * terminalVelocity;

            if (difference >= limit
                    && (fallDistance > significantFallDistance
                    || previous > significantFallDistance)
                    && (difference != prediction
                    || !toGround && !fromGround)) {
                int amplifier = PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP);
                double damage = AlgebraUtils.integerCeil(
                        this.distance
                                - significantFallDistance
                                - (amplifier == -1 ? 0.0f : amplifier + 1.0f)
                );
                synchronized (this.violationObjects) {
                    if (this.violationObjects.size() == TPS.maximum) {
                        this.violationObjects.remove(0);
                    }
                    this.violationObjects.add(new ViolationObject(
                            player,
                            "type: motion"
                                    + ", fall-distance: " + fallDistance
                                    + ", fall-tick: " + fallDistanceTick
                                    + ", prediction: " + prediction
                                    + ", difference: " + difference
                                    + ", limit: " + limit,
                            difference,
                            damage
                    ));
                }
            }
        });
        this.previous = fallDistance;
        this.distance = fallDistanceTick == 1 ? acceleration : this.distance + vertical;
    }

    private int getFallTicks(double fallDistance) {
        if (fallDistance == 0.0) {
            return 0;
        } else {
            double min = Double.MAX_VALUE;
            int found = 0;

            for (Map.Entry<Double, Integer> entry : fallTicksLibrary.entrySet()) {
                double comparison = Math.abs(entry.getKey() - fallDistance);

                if (comparison < min) {
                    min = comparison;
                    found = entry.getValue();
                } else {
                    break;
                }
            }
            return found;
        }
    }

    @Override
    protected void cannotRun(boolean cancelled) {
        this.previous = player.getFallDistance();
        this.distance = 0.0;
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(player, false, false, false, true)
                && player.getVehicle() == null
                && !player.movement.wasInLiquids()
                && !player.trackers.has(Trackers.TrackerType.BOUNCING_BLOCKS)
                && !player.trackers.has(Trackers.TrackerType.BUBBLE_WATER, "soul-sand")
                && (!PlayerUtils.levitation || !player.hasPotionEffect(PotionEffectType.LEVITATION, 0))
                && !BlockPatternUtils.isBlockPattern(new double[]{
                        1,
                        0.0,
                        1
                },
                player.movement.getEventToLocation(),
                true,
                BlockUtils.climbable, BlockUtils.liquid)
                && !BlockPatternUtils.isLiquidPattern(new double[]{
                        1,
                        0.0,
                        1
                },
                player.movement.getEventToLocation(),
                true);
    }

}
