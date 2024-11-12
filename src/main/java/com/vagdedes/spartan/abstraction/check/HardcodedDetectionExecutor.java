package com.vagdedes.spartan.abstraction.check;

import com.vagdedes.spartan.abstraction.profiling.PlayerProfile;
import com.vagdedes.spartan.abstraction.protocol.SpartanProtocol;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.functionality.tracking.PlayerEvidence;
import com.vagdedes.spartan.functionality.tracking.ResearchEngine;

public abstract class HardcodedDetectionExecutor extends CheckDetection {

    public HardcodedDetectionExecutor(CheckRunner executor, String name, boolean def) {
        super(executor, name, def);

        for (Check.DataType dataType : Check.DataType.values()) {
            this.setProbability(dataType, PlayerEvidence.notificationProbability);
        }
    }

    public HardcodedDetectionExecutor(CheckDetection executor, String name, boolean def) {
        super(executor, name, def);

        for (Check.DataType dataType : Check.DataType.values()) {
            this.setProbability(dataType, PlayerEvidence.notificationProbability);
        }
    }

    public final void setHackingRatio(double ratio) {
        if (this.protocol() != null) {
            AwarenessNotifications.forcefullySend("1");
            this.setProbability(
                    this.protocol().spartan.dataType,
                    PlayerEvidence.probabilityToCertainty(ratio)
            );
        } else {
            AwarenessNotifications.forcefullySend("2");
        }
    }

    @Override
    public void clearProbability(Check.DataType dataType) {
    }

    @Override
    protected final boolean hasDataToCompare(Check.DataType dataType) {
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
                    access = ((SpartanProtocol) detected).spartan.getExecutor(
                            hackType
                    ).getDetection(
                            this.name
                    ).surpassedProbability(
                            ((SpartanProtocol) detected).spartan.dataType,
                            PlayerEvidence.notificationProbability
                    );
                } else {
                    PlayerProfile profile = ResearchEngine.getPlayerProfile(detected.toString(), true);
                    access = profile.getExecutor(
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
