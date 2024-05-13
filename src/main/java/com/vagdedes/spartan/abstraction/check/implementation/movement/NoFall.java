package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class NoFall extends CheckExecutor {

    private double[] previous;
    private int fallTicks;

    public NoFall(SpartanPlayer player) {
        super(HackType.NoFall, player);
        this.previous = new double[]{0.0, 0.0};
        this.fallTicks = 0;
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    public void runInternal(boolean cancelled) {
        if (!canDo()) {
            this.previous = new double[]{0.0, 0.0};
            this.fallTicks = 0;
            return;
        }
        Double vertical = player.movement.getNmsVerticalDistance();

        if (vertical == null) {
            this.previous = new double[]{0.0, 0.0};
            this.fallTicks = 0;
            return;
        }
        float fallDistance = player.getFallDistance();
        int fallTick = Math.max(
                player.movement.getFallTick(
                        vertical,
                        -PlayerUtils.airAcceleration,
                        PlayerUtils.airDrag,
                        GroundUtils.maxHeightLengthRatio
                ) - 1,
                0
        );
        List<Double> jumpMotions = PlayerUtils.getJumpMotions(
                PlayerUtils.getPotionLevel(player, PotionEffectType.JUMP) + 1
        );
        double drag = 0.98,
                jumpDifference = jumpMotions.get(jumpMotions.size() - 1) * PlayerUtils.airDrag,
                acceleration = PlayerUtils.airAcceleration * drag,
                prediction1 = fallTick == 0 ? 0.0 :
                        fallTick == 1 ? acceleration :
                                (this.previous[0] * drag) + (fallTick * acceleration),
                prediction2 = fallTick == 0 ? 0.0 :
                        fallTick == 1 ? acceleration - jumpDifference
                                : (this.previous[1] * drag) + (fallTick * acceleration) - jumpDifference,
                difference = Math.min(
                        Math.abs(fallDistance - prediction1),
                        Math.abs(fallDistance - prediction2)
                );

        if (difference >= PlayerUtils.airAcceleration) {
            fallTick = this.fallTicks + 1;
            prediction1 = fallTick == 1 ? acceleration :
                    (this.previous[0] * drag) + (fallTick * acceleration);
            prediction2 = fallTick == 1 ? acceleration - jumpDifference
                    : (this.previous[1] * drag) + (fallTick * acceleration) - jumpDifference;
            difference = Math.min(
                    Math.abs(fallDistance - prediction1),
                    Math.abs(fallDistance - prediction2)
            );

            if (difference >= PlayerUtils.airAcceleration) {
                cancel(
                        "type: motion"
                                + ", fall-distance: " + fallDistance
                                + ", fall-tick: " + fallTick
                                + ", fall-prediction: " + prediction1
                                + ", jump-prediction: " + prediction2
                                + ", difference: " + difference,
                        null,
                        0,
                        false,
                        0.0 // todo damage
                );
            }
        }
        this.previous[0] = prediction1;
        this.previous[1] = prediction2;
        this.fallTicks = fallTick;
    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    protected boolean canDo() { // todo fix non-measurement times
        return MovementProcessing.canCheck(player, false, false, false)
                && player.getVehicle() == null
                && !player.movement.wasInLiquids()
                && !player.getTrackers().has(Trackers.TrackerType.BOUNCING_BLOCKS)
                && !player.getTrackers().has(Trackers.TrackerType.BUBBLE_WATER, "soul-sand")
                && !BlockUtils.canClimb(player.movement.getEventToLocation().getBlock().material, false)
                && (!PlayerUtils.levitation || !player.hasPotionEffect(PotionEffectType.LEVITATION, 0));
    }
}
