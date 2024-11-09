package com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements;

import com.vagdedes.spartan.abstraction.check.CheckExecutor;
import com.vagdedes.spartan.abstraction.check.DetectionExecutor;
import com.vagdedes.spartan.abstraction.check.ImplementedDetectionExecutor;
import com.vagdedes.spartan.abstraction.check.implementation.movement.irregularmovements.gravity.IrregularMovementsGravityNew;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.MovementProcessing;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;
import me.vagdedes.spartan.system.Enums.HackType;

public class IrregularMovements extends CheckExecutor {

    private long teleport;
    public final DetectionExecutor limitServerFlying;
    private final IrregularMovementsStep step;
    private final IrregularMovementsGravity gravityOld;
    private final IrregularMovementsGravityNew gravity;
    private final IrregularMovementsJump jump;
    private final IrregularMovementsHorizontal horizontal;
    private final IrregularMovementsFoodSprint foodSprint;
    private final IrregularMovementsBaritone baritone;

    public IrregularMovements(HackType hackType, SpartanProtocol protocol) {
        super(hackType, protocol);
        limitServerFlying = new ImplementedDetectionExecutor(this, "limit_server_flying", false);
        step = new IrregularMovementsStep(this);
        gravityOld = new IrregularMovementsGravity(this);
        gravity = new IrregularMovementsGravityNew(this);
        jump = new IrregularMovementsJump(this);
        horizontal = new IrregularMovementsHorizontal(this);
        foodSprint = new IrregularMovementsFoodSprint(this);
        baritone = new IrregularMovementsBaritone(this);
    }

    @Override
    protected void runInternal(boolean cancelled) {
        foodSprint.run();
        Double vertical = this.protocol().spartan.movement.getEventVertical(),
                oldVertical = this.protocol().spartan.movement.getPreviousEventVertical(),
                horizontal = this.protocol().spartan.movement.getEventHorizontal(),
                oldHorizontal = this.protocol().spartan.movement.getPreviousEventHorizontal(),
                box = this.protocol().spartan.movement.getEventBox(),
                oldBox = this.protocol().spartan.movement.getPreviousEventBox();

        if (vertical == null
                || oldVertical == null
                || horizontal == null
                || oldHorizontal == null
                || box == null
                || oldBox == null) {
            return;
        }
        boolean knownBox = GroundUtils.blockHeightExists(box),
                knownOldBox = GroundUtils.blockHeightExists(oldBox),
                teleported = teleport >= System.currentTimeMillis();

        if (knownBox || knownOldBox) {
            if (vertical == 0.0) {
                this.horizontal.run(knownBox, knownOldBox, box, oldBox, vertical);
            } else {
                step.run(knownBox, knownOldBox, box, oldBox, vertical, teleported);
            }
        } else {
            gravityOld.run(vertical, oldVertical, box, oldBox);
        }
        if (!teleported) {
            jump.run(vertical, box);
        }
        baritone.run();
        gravity.run();
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        gravity.teleport();
        this.teleport = System.currentTimeMillis()
                + (AlgebraUtils.integerCeil(TPS.maximum / 2.0) * TPS.tickTime);
    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(
                this.protocol(),
                false,
                false,
                true,
                false
        ) && !this.protocol().spartan.movement.wasGliding();
    }

}