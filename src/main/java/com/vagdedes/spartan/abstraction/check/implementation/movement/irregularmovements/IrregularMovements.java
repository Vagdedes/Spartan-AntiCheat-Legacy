package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class IrregularMovements extends CheckExecutor {

    // Base
    public static final HackType check = HackType.IrregularMovements;
    private final IrregularMovementsStep step;
    private final IrregularMovementsGravity gravity;
    private final IrregularMovementsJump jump;

    public IrregularMovements(SpartanPlayer player) {
        super(check, player, 3);
        this.addDetections(new DetectionExecutor[]{
                step = new IrregularMovementsStep(this),
                gravity = new IrregularMovementsGravity(this),
                jump = new IrregularMovementsJump(this)
        });
    }

    @Override
    public boolean runInternal(boolean cancelled) {
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
            return false;
        }
        if (canDo()) {
            if (GroundUtils.blockHeightExists(box) || GroundUtils.blockHeightExists(oldBox)) {
                step.run();
            } else {
                gravity.run(vertical, oldVertical, box, oldBox);
            }
            jump.run(vertical, box);
        }
        return false;
    }

    @Override
    public void scheduler() {

    }

    @Override
    public boolean handleInternal(boolean cancelled, Object object) {
        return false;
    }

    @Override
    protected boolean canDo() {
        return player.canRunCheck(hackType, true)
                && !player.getTrackers().has(Trackers.TrackerType.ElytraUse)
                && (!player.bedrockPlayer || player.movement.getNmsToLocation().getBlockY() > BlockUtils.getMinHeight(player.getWorld()));
    }
}