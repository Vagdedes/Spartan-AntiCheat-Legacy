package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.GroundUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class NoFall extends CheckExecutor {

    private static final double
            drag = 0.98,
            acceleration = PlayerUtils.airAcceleration * drag,
            terminalVelocity = PlayerUtils.calculateTerminalVelocity(
                    PlayerUtils.airDrag,
                    PlayerUtils.airAcceleration
            );
    private static final int maxFallTicks = 500;
    private static final Map<Double, Integer> fallTicksLibrary = new LinkedHashMap<>();

    static {
        double previous = 0.0;

        for (int fallTick = 0; fallTick <= maxFallTicks; fallTick++) {
            double prediction = (previous * drag) + (fallTick * acceleration);
            previous = prediction;
            fallTicksLibrary.put(
                    AlgebraUtils.cut(prediction, MovementProcessing.heightPrecision),
                    fallTick
            );
        }
    }

    private double previousFallDistance, distance;
    private int previousFallTick;

    public NoFall(HackType hackType, SpartanPlayer player) {
        super(hackType, player);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        this.previousFallDistance = player.getFallDistance();
        this.distance = 0.0;
        this.previousFallTick = 0;
    }

    @Override
    public void cannotHandle(boolean cancelled, Object object) {
        this.handleInternal(cancelled, object);
    }

    @Override
    public void runInternal(boolean cancelled) {
        SpartanLocation to = player.movement.getEventToLocation(),
                from = player.movement.getEventFromLocation();
        double vertical = player.movement.getValueOrDefault(player.movement.getEventVertical(), 0.0),
                oldVertical = player.movement.getValueOrDefault(player.movement.getPreviousEventVertical(), 0.0),
                fallDistance = player.getFallDistance();
        int fallDistanceTick = getFallTicks(previousFallDistance);
        boolean ground = player.isOnGround(to, true);

        if (previousFallTick < maxFallTicks
                && (fallDistance > 0.0 || previousFallDistance > 0.0)) {
            runAsync(() -> {
                double prediction = (this.previousFallDistance * drag) + (fallDistanceTick * acceleration),
                        difference = Math.abs(fallDistance - prediction),
                        limit = vertical >= 0.0
                                ? PlayerUtils.airAcceleration
                                : Math.max(0.0 - vertical, PlayerUtils.airAcceleration);

                if (difference >= limit
                        && !player.movement.justLanded(
                        vertical,
                        oldVertical,
                        -PlayerUtils.airAcceleration,
                        PlayerUtils.airDrag,
                        GroundUtils.maxHeightLengthRatio)
                        && (difference != prediction
                        || !ground
                        && !player.isOnGround(to, false)
                        && !player.isOnGround(from, false))) {
                    double damage = AlgebraUtils.integerCeil(
                            this.distance - 3.0f
                                    - (PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1.0f)
                    );
                    cancel(
                            "type: motion"
                                    + ", fall-distance: " + fallDistance
                                    + ", fall-tick: " + fallDistanceTick
                                    + ", prediction: " + prediction
                                    + ", difference: " + difference
                                    + ", limit: " + limit
                                    + ", vertical: " + vertical,
                            difference,
                            null,
                            0,
                            false,
                            damage
                    );
                }
            });
        }
        this.previousFallDistance = fallDistance;
        this.distance += Math.abs(vertical);
        this.previousFallTick = fallDistanceTick;
    }

    private int getFallTicks(double fallDistance) {
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

    @Override
    protected void cannotRun(boolean cancelled) {
        this.handleInternal(cancelled, null);
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
