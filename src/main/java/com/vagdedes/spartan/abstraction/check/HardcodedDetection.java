package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

public abstract class HardcodedDetection extends CheckDetection {

    public HardcodedDetection(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);

        for (Check.DataType dataType : Check.DataType.values()) {
            this.setProbability(dataType, PlayerEvidence.notificationProbability);
        }
    }

    public final void setHackingRatio(double ratio) {
        if (this.protocol() != null) {
            this.setProbability(
                    this.protocol().spartan.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
        }
    }

    @Override
    public void clearProbability(Check.DataType dataType) {
    }

    @Override
    protected final boolean hasData(Check.DataType dataType) {
        return true;
    }

    @Override
    protected final boolean hasSufficientData(Check.DataType dataType) {
        return true;
    }

    @Override
    public final boolean canSendNotification(Object detected, int probability) {
        long time = System.currentTimeMillis();

        if (this.notifications <= time) {
            boolean player = detected instanceof SpartanProtocol;
            int ticks = this.executor.getNotificationTicksCooldown(
                    player ? (SpartanProtocol) detected : null
            );

            if (ticks > 0) {
                boolean access;

                if (player) {
                    access = ((SpartanProtocol) detected).spartan.getRunner(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            ((SpartanProtocol) detected).spartan.dataType,
                            PlayerEvidence.notificationProbability
                    );
                } else {
                    PlayerProfile profile = ResearchEngine.getPlayerProfile(detected.toString(), true);
                    access = profile.getRunner(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            profile.getLastDataType(),
                            PlayerEvidence.notificationProbability
                    );
                }

                if (access) {
                    this.notifications = time + (ticks * TPS.tickTime);
                    return true;
                }
            } else {
                this.notifications = time;
                return true;
            }
        }
        return false;
    }

}
