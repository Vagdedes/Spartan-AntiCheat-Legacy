package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class IrregularMovements extends CheckExecutor {

    // Base
    private final IrregularMovementsStep step;
    private final IrregularMovementsGravity gravity;
    private final IrregularMovementsJump jump;
    private final IrregularMovementsHorizontal horizontal;
    private final IrregularMovementsFoodSprint foodSprint;

    public IrregularMovements(HackType hackType, SpartanPlayer player) {
        super(hackType, player, 5);
        this.addDetections(new DetectionExecutor[]{
                step = new IrregularMovementsStep(this),
                gravity = new IrregularMovementsGravity(this),
                jump = new IrregularMovementsJump(this),
                horizontal = new IrregularMovementsHorizontal(this),
                foodSprint = new IrregularMovementsFoodSprint(this)
        });
    }

    @Override
    protected void runInternal(boolean cancelled) {
        foodSprint.run();
        Double vertical = player.movement.getEventVertical(),
                oldVertical = player.movement.getPreviousEventVertical(),
                horizontal = player.movement.getEventHorizontal(),
                oldHorizontal = player.movement.getPreviousEventHorizontal(),
                box = player.movement.getEventBox(),
                oldBox = player.movement.getPreviousEventBox();

        if (vertical == null
                || oldVertical == null
                || horizontal == null
                || oldHorizontal == null
                || box == null
                || oldBox == null) {
            return;
        }
        boolean knownBox = GroundUtils.blockHeightExists(box),
                knownOldBox = GroundUtils.blockHeightExists(oldBox);

        if (knownBox || knownOldBox) {
            if (vertical == 0.0) {
                this.horizontal.run(knownBox, knownOldBox, box, oldBox, vertical);
            } else {
                this.step.run(knownBox, knownOldBox, box, oldBox, vertical);
            }
        } else {
            this.gravity.run(vertical, oldVertical, box, oldBox);
        }
        jump.run(vertical, box);
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(player, false, true, false, true);
    }

}