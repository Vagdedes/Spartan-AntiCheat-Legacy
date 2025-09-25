package ai.idealistic.spartan.abstraction.check.implementation;

import ai.idealistic.spartan.abstraction.check.CheckDetection;
import ai.idealistic.spartan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.spartan.abstraction.check.CheckRunner;
import ai.idealistic.spartan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.spartan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.spartan.functionality.tracking.CheckConditions;

public class ElytraCheck extends CheckRunner {

    private final CheckDetection detection;

    public ElytraCheck(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(
                this,
                null,
                null,
                null,
                true
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {
        if (object instanceof X) {
            // todo
        }
    }

    @Override
    protected boolean canRun() {
        return CheckConditions.canCheckMovement(
                this.protocol,
                true,
                true,
                false,
                true,
                false
        );
    }

    private void run() {
        this.detection.call(() -> {
            // todo
        });
    }

}