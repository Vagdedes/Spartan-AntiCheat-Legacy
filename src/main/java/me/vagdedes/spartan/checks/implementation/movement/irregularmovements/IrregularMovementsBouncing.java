package me.vagdedes.spartan.checks.implementation.movement.irregularmovements;

import me.vagdedes.spartan.features.performance.AsyncDetection;
import me.vagdedes.spartan.handlers.identifiers.complex.predictable.BouncingBlocks;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.BlockUtils;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.PatternUtils;

public class IrregularMovementsBouncing {

    private static final double[] locations = new double[]{
            BlockUtils.hitbox_max, 2, BlockUtils.hitbox_max
    };

    static void run(SpartanPlayer p, SpartanLocation to, SpartanLocation from,
                    double vertical, double oldVertical, double box,
                    boolean knownBoxes, boolean offGround, boolean airTicksOffGround, boolean heightExistsHalf,
                    boolean canGoUp, boolean canGoDown,
                    boolean jumping, boolean falling,
                    boolean levitationEffect,
                    int airTicks,
                    String prevention, String scenario) {
        if (IrregularMovements.check.getCheck().getBooleanOption("check_bouncing_blocks", true)) {
            AsyncDetection.run(p, IrregularMovements.check, () -> {
                if (!PatternUtils.isBlockPattern(locations, to, true, BlockUtils.solid, BlockUtils.semi_solid)) {
                    if (vertical > 0.0) {
                        if (canGoUp) {
                            boolean canBounce = BlockUtils.isBouncingBlock(p, to, 2),
                                    canBounceLive = false; // Use it to save performance in specific circumstances

                            if (vertical > oldVertical) { // Supposed bounce moment
                                double verticalRemaining = vertical - Math.floor(vertical);

                                if (canBounce) {
                                    if (!levitationEffect
                                            && airTicks > 0
                                            && verticalRemaining != box
                                            && verticalRemaining != (box - GroundUtils.bedStep)) { // Bounce must always match the hitboxes of the block
                                        IrregularMovements.cancel(p, from, prevention, "up-illegal-" + scenario, null, 2, -IrregularMovements.defaultBufferTime);
                                    }
                                } else if (GroundUtils.heightExists(box)
                                        && (canBounceLive = BouncingBlocks.isLiveBelow(p, to))) { // Block boxes can rarely be met when bouncing
                                    IrregularMovements.cancel(p, from, prevention, "box-" + scenario, 2);
                                }
                            } else {
                                double difference = Math.abs(vertical - oldVertical); // Measure the difference and prevent abnormal movements

                                if (!levitationEffect
                                        && difference < 0.08) {
                                    IrregularMovements.cancel(p, from, prevention, "up-below-" + scenario, 3);
                                } else if (difference > 0.16) {
                                    IrregularMovements.cancel(p, from, prevention, "up-above-" + scenario, 3);
                                }
                            }

                            if (!canBounce && !heightExistsHalf && jumping
                                    && (canBounceLive || BouncingBlocks.isLiveBelow(p, to))
                                    && p.getBlocksOffGround(4, true, true) >= 4) {
                                IrregularMovements.cancel(p, from, prevention, "jump-" + scenario, 2);
                            }
                        }
                    } else if (vertical < 0.0) {
                        if (canGoDown && offGround) { // Avoid low ground movements
                            if (vertical > oldVertical) {
                                if (!knownBoxes) {
                                    IrregularMovements.cancel(p, from, prevention, "down-illegal-" + scenario, 2);
                                }
                            } else if (airTicksOffGround // Avoid low ground movements
                                    && oldVertical < 0.0 && !falling // Make sure the player "has been" and is falling
                                    && BouncingBlocks.isLiveBelow(p, to)) {
                                double difference = Math.abs(vertical - oldVertical); // Measure the difference and prevent abnormal movements

                                if (difference >= 0.08) {
                                    IrregularMovements.cancel(p, from, prevention, "down-above-" + scenario, 3);
                                }
                            }
                        }
                    } // Do not create a horizontal detection as it is covered by the horizontal algorithm
                }
            });
        }
    }
}
