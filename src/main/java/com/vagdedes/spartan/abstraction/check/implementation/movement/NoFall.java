package com.vagdedes.spartan.abstraction.check.implementation.movement;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.PlayerUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockPatternUtils;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

public class NoFall extends CheckExecutor {

    private static final double
            drag = AlgebraUtils.cut(PlayerUtils.airDrag, 2),
            acceleration = PlayerUtils.airAcceleration * drag,
            terminalVelocity = PlayerUtils.calculateTerminalVelocity(PlayerUtils.airDrag, PlayerUtils.airAcceleration);
    private static final int maxFallTicks = 1_000;
    private static final Map<Double, Integer> fallTicksLibrary = new LinkedHashMap<>();

    static {
        double previous = 0.0;

        for (int fallTick = 0; fallTick <= maxFallTicks; fallTick++) {
            double prediction = (previous * drag) + (fallTick * acceleration);
            previous = prediction;
            fallTicksLibrary.put(prediction, fallTick);
        }
    }

    private double previousFallDistance, distance;
    private int previousFallTick;
    private boolean ignore;

    public NoFall(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 0, false);
        this.handleInternal(false, null);
    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {
        this.distance = 0.0;
        this.ignore = true;
        this.previousFallTick = Integer.MAX_VALUE;
        this.previousFallDistance = player.getInstance().getFallDistance();
    }

    @Override
    public void cannotHandle(boolean cancelled, Object object) {
        this.handleInternal(cancelled, object);
    }

    @Override
    public void runInternal(boolean cancelled) {
        if (player.isOnGround(false)) {
            this.ignore = false;
            return;
        }
        if (ignore) {
            return;
        }
        Double vertical = player.movement.getEventVertical(),
                oldVertical = player.movement.getPreviousEventVertical();

        if (vertical == null
                || oldVertical == null) {
            this.distance = 0.0;
            this.previousFallTick = Integer.MAX_VALUE;
            this.previousFallDistance = player.getInstance().getFallDistance();
            return;
        }
        double fallDistance = player.getInstance().getFallDistance();
        int fallDistanceTick = getFallTicks(previousFallDistance);

        if (previousFallTick < maxFallTicks
                && (fallDistance > 0.0 || previousFallDistance > 0.0)) {
            double prediction = (this.previousFallDistance * drag) + (fallDistanceTick * acceleration),
                    difference = Math.abs(fallDistance - prediction),
                    limit = (vertical >= 0.0
                            ? player.movement.getAirAcceleration()
                            : Math.max(0.0 - vertical, player.movement.getAirAcceleration()))
                            * (player.bedrockPlayer ? terminalVelocity : 1.0);

            if (difference >= limit
                    && !player.movement.justJumped(vertical)
                    && !player.movement.justFell(vertical)
                    && !player.movement.justLanded(
                    vertical,
                    oldVertical,
                    -player.movement.getAirAcceleration(),
                    PlayerUtils.airDrag,
                    GroundUtils.maxHeightLengthRatio)
                    && (difference != prediction
                    || (!player.bedrockPlayer || vertical != 0.0)
                    && !player.isOnGround(player.movement.getEventToLocation(), true)
                    && !player.isOnGround(player.movement.getEventFromLocation(), false))) {
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
                                + ", vertical: " + vertical
                                + ", box: " + player.movement.getEventBox(),
                        difference,
                        player.movement.getDetectionLocation(),
                        0,
                        false,
                        damage
                );
            }
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
                && player.getInstance().getVehicle() == null
                && !player.isFrozen()
                && !player.movement.isInVoid()
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
                BlockUtils.climbable,
                BlockUtils.interactive_and_passable,
                BlockUtils.slime_block,
                BlockUtils.bed,
                BlockUtils.liquid)
                && !BlockPatternUtils.isBlockPattern(new double[]{
                        1,
                        -1.0,
                        1
                },
                player.movement.getEventToLocation(),
                true,
                BlockUtils.interactive_and_passable,
                BlockUtils.slime_block,
                BlockUtils.bed)
                && !BlockPatternUtils.isLiquidPattern(new double[]{
                        1,
                        0.0,
                        1
                },
                player.movement.getEventToLocation(),
                true);
    }

}
