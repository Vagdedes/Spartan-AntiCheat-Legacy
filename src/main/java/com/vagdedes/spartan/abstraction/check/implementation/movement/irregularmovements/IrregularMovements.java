package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class IrregularMovements extends CheckExecutor {

    // Base
    private final IrregularMovementsStep step;
    private final IrregularMovementsGravity gravity;
    private final IrregularMovementsJump jump;
    private final IrregularMovementsHorizontal horizontal;

    public IrregularMovements(SpartanPlayer player) {
        super(HackType.IrregularMovements, player, 4);
        this.addDetections(new DetectionExecutor[]{
                step = new IrregularMovementsStep(this),
                gravity = new IrregularMovementsGravity(this),
                jump = new IrregularMovementsJump(this),
                horizontal = new IrregularMovementsHorizontal(this)
        });
    }

    @Override
    public void runInternal(boolean cancelled) {
        Double vertical = player.movement.getNmsVerticalDistance(),
                oldVertical = player.movement.getPreviousNmsVerticalDistance(),
                horizontal = player.movement.getNmsHorizontalDistance(),
                oldHorizontal = player.movement.getPreviousNmsHorizontalDistance(),
                box = player.movement.getNmsBox(),
                oldBox = player.movement.getPreviousNmsBox();

        if (vertical == null
                || oldVertical == null
                || horizontal == null
                || oldHorizontal == null
                || box == null
                || oldBox == null) {
            return;
        }
        if (canDo()) {
            boolean knownBox = GroundUtils.blockHeightExists(box),
                    knownOldBox = GroundUtils.blockHeightExists(oldBox);

            if (knownBox || knownOldBox) {
                if (vertical == 0.0) {
                    this.horizontal.run(knownBox, knownOldBox, box, oldBox, vertical);
                } else {
                    step.run(knownBox, knownOldBox, box, oldBox, vertical);
                }
            } else {
                gravity.run(vertical, oldVertical, box, oldBox);
            }
            jump.run(vertical, box);
        }
    }

    @Override
    public void schedulerInternal() {

    }

    @Override
    public void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    protected boolean canDo() {
        return MovementProcessing.canCheck(player, false, true, false);
    }
}