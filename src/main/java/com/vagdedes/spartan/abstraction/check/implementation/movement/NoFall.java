package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.server.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.server.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class NoFall extends CheckExecutor {

    private static final double
            drag = 0.98,
            acceleration = PlayerUtils.airAcceleration * drag;
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

    private double previous;
    private double distance;

    public NoFall(SpartanPlayer player) {
        super(HackType.NoFall, player);
        this.previous = 0.0;
        this.distance = 0.0;
    }

    @Override
    public void runInternal(boolean cancelled) {
        Double vertical = player.movement.getNmsVerticalDistance();
        double fallDistance = player.getFallDistance();

        if (vertical == null) {
            this.previous = fallDistance;
            this.distance = 0.0;
            return;
        }
        int fallTick = getFallTicks(previous) + 1;
        boolean toGround = player.isOnGround(),
                fromGround = player.isOnGround(player.movement.getEventFromLocation());

        runAsync(() -> {
            double prediction = fallTick == 1
                    ? acceleration
                    : (this.previous * drag) + (fallTick * acceleration),
                    difference = Math.abs(fallDistance - prediction);

            if (difference >= PlayerUtils.airAcceleration
                    && (fallDistance > significantFallDistance
                    || previous > significantFallDistance)
                    && (difference != prediction
                    || !toGround && !fromGround)) {
                int amplifier = PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP);
                cancel(
                        "type: motion"
                                + ", fall-distance: " + fallDistance
                                + ", fall-tick: " + fallTick
                                + ", prediction: " + prediction
                                + ", difference: " + difference,
                        difference,
                        null,
                        0,
                        false,
                        AlgebraUtils.integerCeil(
                                this.distance
                                        - significantFallDistance
                                        - (amplifier == -1 ? 0.0f : amplifier + 1.0f)
                        )
                );
            }
        });
        this.previous = fallDistance;
        this.distance = fallTick == 1 ? acceleration : this.distance + vertical;
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
    public void cannotRun(boolean cancelled) {
        this.previous = player.getFallDistance();
        this.distance = 0.0;
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(player, false, false, false)
                && player.getVehicle() == null
                && !player.movement.wasInLiquids()
                && !player.getTrackers().has(Trackers.TrackerType.BOUNCING_BLOCKS)
                && !player.getTrackers().has(Trackers.TrackerType.BUBBLE_WATER, "soul-sand")
                && !BlockUtils.canClimb(player.movement.getEventToLocation().getBlock().material, false)
                && (!PlayerUtils.levitation || !player.hasPotionEffect(PotionEffectType.LEVITATION, 0));
    }
}
