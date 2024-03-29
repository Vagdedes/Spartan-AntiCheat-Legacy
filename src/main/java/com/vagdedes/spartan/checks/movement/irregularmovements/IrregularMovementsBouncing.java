package com.vagdedes.spartan.checks.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.functionality.identifiers.complex.predictable.BouncingBlocks;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PatternUtils;

public class IrregularMovementsBouncing extends DetectionExecutor {

    private final IrregularMovements irregularMovements;

    IrregularMovementsBouncing(CheckExecutor executor) {
        super(executor, executor.hackType, executor.player);
        irregularMovements = (IrregularMovements) executor;
    }

    private static final double[] locations = new double[]{
            BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max
    };

    void run(boolean knownBoxes, boolean offGround, boolean airTicksOffGround, boolean heightExistsHalf,
             boolean canGoUp, boolean canGoDown,
             boolean jumping, boolean falling,
             boolean levitationEffect,
             String prevention, String scenario) {
        if (hackType.getCheck().getBooleanOption("check_bouncing_blocks", true)) {
            Double vertical = player.movement.getNmsVerticalDistance(),
                    oldVertical = player.movement.getPreviousNmsVerticalDistance(),
                    box = player.movement.getNmsBox();

            if (vertical == null
                    || oldVertical == null
                    || box == null) {
                return;
            }
            SpartanLocation to = player.getEventToLocation();
            int airTicks = player.movement.getTicksOnAir();

            runAsync(() -> {
                if (!PatternUtils.isBlockPattern(locations, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                    if (vertical > 0.0) {
                        if (canGoUp) {
                            boolean canBounce = BlockUtils.isBouncingBlock(player, to, 2),
                                    canBounceLive = false; // Use it to save performance in specific circumstances

                            if (vertical > oldVertical) { // Supposed bounce moment
                                double verticalRemaining = vertical - Math.floor(vertical);

                                if (canBounce) {
                                    if (!levitationEffect
                                            && airTicks > 0
                                            && verticalRemaining != box
                                            && verticalRemaining != (box - GroundUtils.bedStep)) { // Bounce must always match the hitboxes of the block
                                        irregularMovements.cancel(prevention, "up-illegal-" + scenario, null, 2, -IrregularMovements.defaultBufferTime);
                                    }
                                } else if (GroundUtils.heightExists(box)
                                        && (canBounceLive = BouncingBlocks.isLiveBelow(player, to))) { // Block boxes can rarely be met when bouncing
                                    irregularMovements.cancel(prevention, "box-" + scenario, 2);
                                }
                            } else {
                                double difference = Math.abs(vertical - oldVertical); // Measure the difference and prevent abnormal movements

                                if (!levitationEffect
                                        && difference < 0.08) {
                                    irregularMovements.cancel(prevention, "up-below-" + scenario, 3);
                                } else if (difference > 0.16) {
                                    irregularMovements.cancel(prevention, "up-above-" + scenario, 3);
                                }
                            }

                            if (!canBounce && !heightExistsHalf && jumping
                                    && (canBounceLive || BouncingBlocks.isLiveBelow(player, to))
                                    && player.getBlocksOffGround(4) >= 4) {
                                irregularMovements.cancel(prevention, "jump-" + scenario, 2);
                            }
                        }
                    } else if (vertical < 0.0) {
                        if (canGoDown && offGround) { // Avoid low ground movements
                            if (vertical > oldVertical) {
                                if (!knownBoxes) {
                                    irregularMovements.cancel(prevention, "down-illegal-" + scenario, 2);
                                }
                            } else if (airTicksOffGround // Avoid low ground movements
                                    && oldVertical < 0.0 && !falling // Make sure the player "has been" and is falling
                                    && BouncingBlocks.isLiveBelow(player, to)) {
                                double difference = Math.abs(vertical - oldVertical); // Measure the difference and prevent abnormal movements

                                if (difference >= 0.08) {
                                    irregularMovements.cancel(prevention, "down-above-" + scenario, 3);
                                }
                            }
                        }
                    } // Do not create a horizontal detection as it is covered by the horizontal algorithm
                }
            });
        }
    }
}
