package me.vagdedes.spartan.checks.movement.irregularmovements;

import me.vagdedes.spartan.handlers.stability.TPS;
import me.vagdedes.spartan.objects.data.Buffer;
import me.vagdedes.spartan.objects.replicates.SpartanLocation;
import me.vagdedes.spartan.objects.replicates.SpartanPlayer;
import me.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.utils.gameplay.MoveUtils;

public class IrregularMovementsMotion {

    private static final double criticalStepLimit = 0.5 * Math.round((GroundUtils.maxPlayerStep * 2.0) / 0.5); // 1.5

    static void run(SpartanPlayer p,
                    SpartanLocation to, SpartanLocation from,
                    double vertical, double oldVertical, double verticalDifference, double oldVerticalDifference,
                    double box, double oldBox,
                    boolean knownBoxes, boolean knownBoxesNew, boolean knownBoxesOld,
                    boolean onGroundCustom,
                    boolean groundInteractionScenario, boolean velocityScenario,
                    boolean bouncingBlocks,
                    boolean checkUp, boolean checkDown, boolean checkJump, boolean checkFall, boolean checkHorizontal,
                    int airTicks,
                    String prevention) {
        checkUp &= IrregularMovements.check.getCheck().getBooleanOption("check_upwards", true);
        checkDown &= IrregularMovements.check.getCheck().getBooleanOption("check_downwards", true);
        checkJump &= IrregularMovements.check.getCheck().getBooleanOption("check_jumping", true);
        checkFall &= IrregularMovements.check.getCheck().getBooleanOption("check_falling", true);
        checkHorizontal &= IrregularMovements.check.getCheck().getBooleanOption("check_horizontal", true);
        double stepLimit = IrregularMovements.check.getCheck().getDecimalOption("step_limit_in_blocks", GroundUtils.maxPlayerStep);

        if (stepLimit > 0.0) { // Step
            if (knownBoxesNew
                    && knownBoxesOld) { // Step
                double boxDifference = Math.abs((to.getBlockY() + box) - (oldBox + from.getBlockY()));

                if (vertical > 0.0) {
                    if (boxDifference > stepLimit) {
                        IrregularMovements.cancel(p, from, prevention, "step-up-predictable", "box-difference: " + boxDifference, boxDifference >= criticalStepLimit ? 1 : 2, IrregularMovements.defaultBufferTime);
                    }
                } else if (vertical < 0.0
                        && boxDifference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Anything below this threshold is likely instant, where otherwise you fall for a bit
                    IrregularMovements.cancel(p, from, prevention, "step-down-predictable", 1);
                }
            } else if (knownBoxesNew) {
                if (vertical > stepLimit) {
                    IrregularMovements.cancel(p, from, prevention, "step-up-predictable", vertical >= criticalStepLimit ? 1 : 2);
                } else if (vertical <= -(MoveUtils.falling[2] + MoveUtils.highPrecision)) {
                    p.debug(false, false, "step-down-known", vertical, oldVertical, "|", verticalDifference,oldVerticalDifference);
                }
            } else {
                //todo
            }
        }

        if (vertical < oldVertical) { // Fall
            if (p.isJumping(vertical)) {
                if (checkJump
                        && p.isJumping(oldVertical)) {
                    double difference = Math.abs(verticalDifference);

                    if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                        IrregularMovements.cancel(p, from, prevention, "jump-up-skip", 1);
                    } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                        IrregularMovements.cancel(p, from, prevention, "jump-up-equilibrium", groundInteractionScenario ? 3 : 1);
                    }
                }
            } else if (p.isJumping(oldVertical)) {
                if (checkJump
                        && Math.abs(oldVertical - MoveUtils.jumping[0]) >= MoveUtils.jumping[2]) { // Jumping must end somewhere
                    IrregularMovements.cancel(p, from, prevention, "jump-end", groundInteractionScenario || velocityScenario ? 3 : 1);
                }
            } else if (!(checkFall || checkDown) && !onGroundCustom) {
                //todo
            }
        } else if (vertical > oldVertical) { // Ascension
            if (p.isJumping(vertical)) {
                if (checkJump) {
                    if (p.isJumping(oldVertical)) {
                        double difference = Math.abs(verticalDifference);

                        if (difference >= (MoveUtils.maxJumpingMotionDifference + MoveUtils.highPrecision)) { // Jumping motions must not skip each other
                            IrregularMovements.cancel(p, from, prevention, "jump-down-skip", groundInteractionScenario ? 3 : 1);
                        } else if (difference < MoveUtils.highPrecision) { // Jumping motions must not be the same
                            IrregularMovements.cancel(p, from, prevention, "jump-down-equilibrium", groundInteractionScenario ? 3 : 1);
                        }
                    } else if (!onGroundCustom
                            && !GroundUtils.heightExists(box - vertical, true)
                            && Math.abs(vertical - MoveUtils.jumping[1]) < MoveUtils.jumping[2]) { // Jumping must start from somewhere
                        IrregularMovements.cancel(p, from, prevention, "jump-start", 1);
                    }
                }
            } else if (p.isJumping(oldVertical)) {
                if (checkJump) {
                    p.debug(false, false, "jump", vertical, oldVertical, box, oldBox);
                }
                // check if it's negative or zero
            } else if (checkUp && !onGroundCustom) {
                //todo
            }
        } else if (checkHorizontal && !onGroundCustom) { // Equilibrium
            p.debug(false, false, "horizontal", vertical, oldVertical, verticalDifference, oldVerticalDifference);
        }

        if (p.getCooldowns(IrregularMovements.check).has("jump-start")) {
            p.debug(false, false, airTicks, onGroundCustom, vertical);
        }
    }
}
