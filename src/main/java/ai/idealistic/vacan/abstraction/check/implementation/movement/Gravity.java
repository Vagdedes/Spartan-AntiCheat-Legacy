package ai.idealistic.vacan.abstraction.check.implementation.movement;

import ai.idealistic.vacan.abstraction.check.CheckDetection;
import ai.idealistic.vacan.abstraction.check.CheckEnums.HackType;
import ai.idealistic.vacan.abstraction.check.CheckRunner;
import ai.idealistic.vacan.abstraction.check.definition.ImplementedDetection;
import ai.idealistic.vacan.abstraction.protocol.PlayerProtocol;
import ai.idealistic.vacan.functionality.tracking.MovementProcessing;

public class Gravity extends CheckRunner {

    private final CheckDetection detection;

    public Gravity(HackType hackType, PlayerProtocol protocol) {
        super(hackType, protocol);
        this.detection = new ImplementedDetection(
                this,
                null,
                null,
                "check_gravity",
                true
        );
    }

    @Override
    protected void handleInternal(boolean cancelled, Object object) {

    }

    @Override
    protected boolean canRun() {
        return MovementProcessing.canCheck(
                this.protocol,
                true,
                true,
                false,
                true,
                false
        );
    }

}